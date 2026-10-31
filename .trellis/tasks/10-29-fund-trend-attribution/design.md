# 重复度趋势与归因看板 — 技术设计

## 1. 数据来源与口径

只读以下表，不新增业务表：

- `duplicate_snapshot`：项目级月度指标（max_score、avg_top_score、归因计数、risk_exposure）。
- `analysis_pair`：项目对明细（relation、direction、earlier_project_id、total_score、level）。
- `analysis_run`：确定月份最新运行。
- `project`、`sys_group`、`tech_direction`：维度与过滤。

### 月份去重

```sql
-- 每个项目每月只取最新 run 的快照
SELECT s.*
FROM duplicate_snapshot s
JOIN (
  SELECT project_id, DATE_FORMAT(analysis_date,'%Y-%m') AS ym, MAX(run_id) AS max_run
  FROM duplicate_snapshot
  GROUP BY project_id, DATE_FORMAT(analysis_date,'%Y-%m')
) t ON s.project_id=t.project_id AND s.run_id=t.max_run
```

H2 兼容写法：使用 `FORMATDATETIME(analysis_date,'yyyy-MM')` 或避免函数——改为在应用层按 `analysis_date` 的 `YearMonth` 分组，SQL 只取原始快照。**采用应用层聚合**，避免方言差异：

1. 查询范围内全部快照（带 project_id、analysis_date、run_id）。
2. Java 按 `(projectId, YearMonth)` 取 `run_id` 最大的一条。
3. 再按 `YearMonth` 聚合指标。
4. 项目对归因同理：只统计选中项目的快照对应 run 中的 pair，避免跨 run 重复。

数据量评估：60 项目 × 24 月 = 1440 行，内存聚合可接受；超过 5 万行再下推 SQL。

### 归因视角

- `analysis_pair` 存 `earlier_project_id` 与中性 `direction`（`A_EARLIER/B_EARLIER/SIMULTANEOUS/SELF_GROUP`）。
- 查询某项目 `P` 的 pair 时：若 `P == earlier_project_id` → `OUR_EARLIER`；否则 `PEER_EARLIER`（跨组）；同组一律 `SELF_GROUP`。
- 趋势按“查看项目”统计：每条 pair 在其 `project_id` 视角下计 1 次；跨组对会在双方各计 1 次，属预期（分组视角），导出说明中注明“项目视角，跨组对双方各计一次”。
- 若需“去重项目对”口径，提供 `dedupPairs=true` 参数：按 `min(projectId,peerId)-max(...)` 去重，归因取较晚项目视角。

## 2. 后端组件

```text
insight/trend/
  TrendController
  TrendQueryService        过滤、数据范围、月份去重、聚合
  AttributionService       归因计数与占比、按组矩阵
  HeatmapService           项目组×月份、技术方向×项目组
  TrendSummaryService      规则摘要
  TrendCsvExporter
  dto/ TrendPointVO, AttributionVO, HeatmapVO, TrendQuery
```

### 查询参数

```text
startMonth=2025-01&endMonth=2026-10&granularity=MONTH
groupId?&techDirectionId?&fundType?&level?
dedupPairs=false
```

- `groupId` 对 `GROUP_USER` 强制覆盖为当前组。
- 参数校验：时间跨度 ≤36 个月；未知枚举返回 400。
- 默认时间：最近 12 个月。

## 3. 聚合算法

```text
snapshots = loadSnapshots(range, filters)        // join project 获取 group/direction/fund
dedup = groupBy(projectId, yearMonth).maxBy(runId)
points = groupBy(yearMonth):
  analyzedProjectCount = count(dedup)
  avgMaxScore = avg(snapshot.maxScore)
  avgTopScore = avg(snapshot.avgTopScore)
  highRiskCount = count(maxScore >= highThreshold)
  pairCount = count(pairs of selected runs)
  highPairCount = count(level == HIGH)
  attribution = count by resolved direction
  ratio = count / pairCount
  moM = compare with previous point（存在且基数>0）
```

- 阈值来自当前 active `analysis_config`；历史快照等级已落库，趋势“高风险数”优先用快照中落的 `risk_level`/计数，避免阈值变更导致历史漂移。
- 摘要规则：

```text
if points.size < 2: "数据不足以判断趋势"
delta = last.avgMaxScore - prev.avgMaxScore
趋势词 = delta > 1 ? 上升 : delta < -1 ? 下降 : 基本持平
主归因 = max(attribution counts)
重点关注 = top2 groups by highRiskCount or crossGroupCount
```

## 4. 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/insight/trend` | 趋势点 + 摘要 |
| GET | `/api/v1/insight/attribution` | 归因汇总 + 按组矩阵 |
| GET | `/api/v1/insight/heatmap` | `dimension=GROUP_MONTH|DIRECTION_GROUP` |
| GET | `/api/v1/insight/trend/projects` | 时间点下钻项目列表 |
| GET | `/api/v1/insight/trend/export` | CSV |

响应示例：

```json
{"hasData":true,"granularity":"MONTH","points":[
 {"period":"2026-05","analyzedProjectCount":18,"avgMaxScore":62.4,
  "avgTopScore":51.2,"highRiskCount":3,"pairCount":41,"highPairCount":5,
  "attribution":{"selfGroup":12,"peerEarlier":9,"ourEarlier":7,"simultaneous":13},
  "ratios":{"selfGroup":0.29,"peerEarlier":0.22,"ourEarlier":0.17,"simultaneous":0.32},
  "mom":{"avgMaxScore":0.08}}],
 "summary":["本期平均最高重复度较上期上升 8%…"]}
```

- CSV 列：period、指标、归因计数、口径说明；UTF-8 BOM 保证 Excel 中文正常。

## 5. 前端

```text
views/trend/TrendView.vue
components/trend/FilterBar.vue
components/trend/TrendChart.vue        # 折线（平均分）+ 柱（高风险数）
components/trend/AttributionChart.vue  # 100% 堆叠柱
components/trend/HeatmapChart.vue      # ECharts heatmap
components/trend/DrillDrawer.vue       # 项目列表/项目对
components/trend/SummaryPanel.vue
```

- 状态同步：筛选条件 ↔ URL query（`useRoute/useRouter`），刷新可复现。
- 图表容器统一监听 `ResizeObserver`，销毁时 `dispose`。
- 颜色：四类归因固定色（本组 `#1f5eff`、他组在先 `#f5a524`、我方在先 `#00c2b8`、同期 `#8b93a7`），色盲友好并配图例文字。
- 空态：`hasData=false` 展示引导“先运行一次分析”。
- 下钻抽屉：表格 + 项目链接；项目对用并排对比卡片展示重复/差异片段。
- 导出按钮：调用导出接口，文件名含筛选摘要。

## 6. 权限

- `@PreAuthorize("hasAnyRole('PMO','ADMIN','GROUP_USER')")`。
- `GROUP_USER` 的 `groupId` 由服务端覆盖；前端筛选禁用。
- `EXPERT` 菜单隐藏 + 接口 403。
- 下钻项目详情复用 `ProjectQueryService` 的数据范围校验。

## 7. 测试

| 测试 | 内容 |
|------|------|
| MonthDedupTest | 同项目同月多 run 只取最新；跨月保留 |
| TrendAggregationTest | 平均值、高风险数、环比、空数据 |
| AttributionTest | 四类计数、占比合计、视角换算、去重口径 |
| HeatmapTest | 分组矩阵、无数据单元 |
| ScopeTest | GROUP_USER 强制本组；EXPERT 403 |
| CsvExportTest | 列、BOM、筛选一致 |
| 前端 | 筛选联动、URL 复现、空态、resize |

## 8. 风险

- 口径误解：跨组对双方各计一次；页面与导出均注明，避免被质疑数据不一致。
- 阈值变更导致历史波动：高风险数使用快照已落库结果；平均分不受影响。
- 大数据量：应用层聚合在 5 万行内可控；超限时改为 SQL 分组或预聚合表（记录扩展点）。
- 图表性能：单页最多 36 个点 × 多系列，ECharts 可承受；避免逐点请求。

# 趋势与归因看板契约（A4 实测）

## 1. Scope / Trigger

修改趋势/归因/热力图聚合、下钻、CSV 导出、演示历史数据回填时适用。A4 只读 A3 结果，不重新计算相似度；下游投入（B1）与决策（B2）复用本口径。

## 2. Signatures

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/insight/trend` | PMO/ADMIN/GROUP_USER | 趋势点 + 摘要 |
| GET | `/api/v1/insight/attribution` | 同上 | 四类计数/占比 + 按组矩阵 |
| GET | `/api/v1/insight/heatmap` | 同上 | `dimension=GROUP_MONTH\|DIRECTION_GROUP`、`metric=avg\|highRisk` |
| GET | `/api/v1/insight/trend/projects` | 同上 | 下钻项目列表（需 `period`） |
| GET | `/api/v1/insight/trend/export` | 同上 | CSV（UTF-8 BOM） |

`EXPERT` 由 `@PreAuthorize` 拒绝（403）；未登录 401。

### 关键类

```text
TrendController
TrendQueryService.trend/drill/countUniquePairs
AttributionService.attribution
HeatmapService.heatmap
TrendSummaryService.summarize
TrendDataLoader.load/dedup/rangeOf
TrendCsvExporter.export
DemoTrendSeedRunner（demo profile）
```

## 3. Contracts

### 时间与去重

- `startMonth`/`endMonth` 为 `yyyy-MM`，缺省最近 12 个月；跨度 ≤36 个月，起止倒置或粒度非法返回 400。
- 粒度 `MONTH/QUARTER/YEAR`；period 形如 `2026-05` / `2026-Q2` / `2026`。
- 同一项目在同一**时间粒度**内只保留 `runId` 最大的一次快照；跨粒度/跨月保留历史。
- 月份取快照 `analysis_date`（DATE）直接转 `YearMonth`，不依赖 JVM 默认时区；只有“当前月”默认值使用 `Asia/Shanghai`。

### 指标口径（A4 锁定）

- `analyzedProjectCount`：去重后的快照数。
- `avgMaxScore` / `avgTopScore`：去重快照均值，保留 2 位。
- `highRiskCount = count(high_pair_count > 0)`；**不按当前阈值回算**，调整阈值不改变历史。
- `pairCount = Σ(self_group_count + cross_group_count)`（项目视角）。
- `uniquePairCount`：选定运行中 `min(projectId,peerId)-max(...)` 去重后的对数。
- `highPairCount = Σ(high_pair_count)`（项目视角）。
- 四类归因直接汇总快照的 `self/our/peer/simultaneous` 计数；四类之和等于 `pairCount`。
- 占比保留 4 位；环比首点为 `null`，上期为 0 时百分比为 `null`。

### 下钻与等级

- `period` 必填且与粒度匹配，否则 400。
- 下钻列表的 `level` 按**当前阈值**折算：`HIGH` = `high_pair_count > 0`；`MEDIUM/LOW/NONE` 按 `max_score` 与当前 low/medium 比较。
- 下钻条件写入 URL query（`period`），刷新/分享可复现；关闭抽屉移除 `period`。
- 趋势顶部筛选不提供 level，避免与“按当时阈值判定”冲突。

### 权限与范围

- `GROUP_USER`：服务端强制覆盖 `groupId=本组`，忽略请求参数；前端项目组筛选禁用。
- `PMO/ADMIN`：全量，可按任意组筛选。
- 导出与图表使用完全相同的过滤与数据范围。

### 导出

- UTF-8 BOM；文件首部为 `#` 注释行（生成时间、范围、口径），随后是表头与数据行。
- 表头：`period,analyzedProjectCount,avgMaxScore,avgTopScore,highRiskCount,pairCount,uniquePairCount,highPairCount,selfGroup,peerEarlier,ourEarlier,simultaneous`。
- `Content-Type: text/csv;charset=UTF-8`，`filename*=UTF-8''trend-<start>_<end>.csv`。

### 演示历史数据

- `DemoTrendSeedRunner`：仅 demo profile、`app.demo.trend-seed=true`、非 test profile 时执行；按 `DEMO-TREND-` 前缀幂等。
- 生成 8 个已提交项目，`submit_time` 回填近 8 个月，覆盖 4 个项目组、4 个技术方向、4 对近似主题。
- `scripts/backfill-trend.ps1` 以 ADMIN 身份逐月调用 `POST /analysis/runs {triggerType:FULL, analysisDate}`，生成真实月度快照；可重复执行（同月保留多 run，趋势取最新）。
- A3 的 `analysisDate` 运行**不回写** `project.duplicate_score/risk_level`，避免历史回填覆盖最新结论。

## 4. Validation & Error Matrix

| 条件 | HTTP | 错误码 |
|------|------|--------|
| 月份格式/起止/跨度/粒度非法 | 400 | `40000` |
| 下钻缺少 period | 400 | `40000` |
| heatmap dimension 非法 | 400 | `40000` |
| 下钻 level 非法 | 400 | `40000` |
| EXPERT 访问 | 403 | `40301` |
| 未登录 | 401 | `40100` |

## 5. Good / Base / Bad

- Good：8 个月数据、四类归因齐全，`pairCount = 四类之和`，`uniquePairCount ≤ pairCount`，页面/CSV/下钻数字一致。
- Base：无数据时 `hasData=false`、`points=[]`、摘要提示"暂无可分析数据"，HTTP 200。
- Bad：跨组对在明细存储双向导致 `pairCount` 翻倍；按当前阈值重算历史高风险数导致曲线跳变。
- Bad：`GROUP_USER` 传他组 `groupId` 就能看到全所数据。

## 6. Tests Required

- 单元：月份/粒度去重、均值与环比、空数据、四类计数与占比、按组矩阵、时区边界。
- 集成：权限（GROUP_USER 强制本组、EXPERT 403、未登录 401）、筛选联动、下钻等级、CSV BOM/表头/口径、参数校验。
- 前端：typecheck/build；浏览器验证图表渲染、下钻抽屉、URL 复现、筛选切换、控制台无错误。
- 性能：`TrendPerformanceTest` `@Tag("perf")` 默认排除；手动：
  `mvn test "-Dtest=TrendPerformanceTest" "-Dtest.excludedGroups=none"`。
- A4 实测：137 项测试通过；60 项目 × 24 月聚合 133ms（门禁 500ms）。

## 7. Wrong vs Correct

### Wrong：按当前阈值重算历史高风险

```java
boolean high = snapshot.getMaxScore().compareTo(BigDecimal.valueOf(activeHigh)) >= 0;
```

### Correct：使用落库判定

```java
boolean high = snapshot.getHighPairCount() != null && snapshot.getHighPairCount() > 0;
```

### Wrong：筛选回填触发路由循环

```ts
onBeforeRouteUpdate(() => { readQuery() })        // 读取旧 route.query，重新打开/改筛选
monthRange.value = [start, end]                    // 每次赋新数组，触发 watcher
```

### Correct：使用目标路由 + 值比较

```ts
onBeforeRouteUpdate((to) => { readQuery(to.query) })
if (monthRange.value[0] !== start) { monthRange.value = [start, end] }
```

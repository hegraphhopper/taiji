## 0. 实施口径（B1 锁定）

- 模块：`com.taiji.fund.insight.investment`（只读聚合，不新增迁移）。
- 行模型：项目 × 最新快照；状态限 `APPROVED/IN_PROGRESS/COMPLETED`。
- 未分析：`unanalyzedCount` 单列，平均分分母排除。
- 年份：`start_date` 年度，缺失回退 `created_at`。
- 高风险：`max_score >= 当前 high`；敞口 `budget × max_score / 100`。
- 同比：最新年度 vs 上一年度，缺失返回 null。
- 复用 `TrendScope` 与 A4 的权限/过滤；导出使用 POI `SXSSFWorkbook`。
# 技术创新投入态势分析 — 技术设计

## 1. 数据与口径实现

数据源：`project` + `sys_group` + `tech_direction` + `duplicate_snapshot`（最新有效快照）。

```java
Set<ProjectStatus> included = includeCompleted
  ? {APPROVED, IN_PROGRESS, COMPLETED}
  : {APPROVED, IN_PROGRESS};

// 最新快照：按 project_id 取 analysis_date 最新、run_id 最大
// 无快照 -> score=0, analyzed=false
```

指标：

```text
totalBudget      = Σ budget
projectCount     = count(distinct project)
avgBudget        = totalBudget / projectCount
avgMaxScore      = Σ score / count(analyzed projects)      // 未分析不计入
highRiskCount    = count(score >= high)
riskExposure     = Σ (budget × score / 100)
highRiskExposure = Σ (budget × score / 100) where score >= high
yoy(field)       = prev == 0 ? null : (curr - prev) / prev
```

- 金额使用 `BigDecimal`，`HALF_UP` 保留 2 位；展示到元。
- 时间维度：`start_date` 的年份；缺失用 `created_at`。
- 前端可切换“万元”，仅展示层换算。

## 2. 后端组件

```text
insight/investment/
  InvestmentController
  InvestmentQueryService
  RiskExposureService
  InvestmentExcelExporter
  dto/ InvestmentOverviewVO, DimensionItemVO, RiskProjectVO, InvestmentQuery
```

- 查询一次性拉取筛选范围内项目 + 最新快照（约 60–1000 行），在内存聚合，避免方言与多次查询。
- 分组键：`year`、`fundType`、`groupId`、`techDirectionId`。
- 排序：金额降序；技术方向取 Top 10，其余合并为“其他”。
- 同比：按筛选范围内当前年度与上一年度分别聚合。

## 3. 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/insight/investment/overview` | KPI + 同比 |
| GET | `/api/v1/insight/investment/by-dimension` | `dimension=YEAR|FUND_TYPE|GROUP|TECH_DIRECTION` |
| GET | `/api/v1/insight/investment/risk-projects` | Top N 风险敞口项目 |
| GET | `/api/v1/insight/investment/scatter` | 散点数据（预算/分数/等级/组） |
| GET | `/api/v1/insight/investment/export` | Excel/CSV |

响应示例：

```json
{"totalBudget":86200000,"projectCount":57,"ongoingCount":21,"completedCount":12,
 "avgBudget":1512280.70,"avgMaxScore":58.3,"highRiskCount":9,
 "riskExposure":21854000.00,"highRiskExposure":14320000.00,
 "analyzedCount":54,"unanalyzedCount":3,
 "yoy":{"totalBudget":0.12,"projectCount":0.05}}
```

- 参数：`startYear/endYear`、`fundType`、`groupId`、`techDirectionId`、`includeCompleted`。
- 数据范围：`GROUP_USER` 强制本组；`EXPERT` 403。

## 4. 前端

```text
views/investment/InvestmentView.vue
components/investment/KpiCards.vue
components/investment/YearTrendChart.vue
components/investment/FundTypeChart.vue
components/investment/GroupCompareChart.vue
components/investment/TechDirectionChart.vue   # 矩形树图/条形
components/investment/RiskScatterChart.vue
components/investment/RiskProjectTable.vue
components/investment/DrillDrawer.vue
```

- 图表复用 `ChartContainer`（resize、loading、empty）。
- 数字格式：金额千分位；大额在 KPI 显示“万元/亿元”并保留原始 tooltip。
- 颜色语义：投入蓝、风险橙红、结项灰绿；风险等级色与趋势页一致。
- 散点点击 → 项目抽屉 → 跳转详情。
- 空数据：KPI 显示 0，图表显示空态，不报错。

## 5. 导出

- Excel 使用 POI `SXSSFWorkbook`：Sheet1 KPI 与口径、Sheet2 年度、Sheet3 项目组、Sheet4 技术方向、Sheet5 Top 风险项目。
- CSV 使用 UTF-8 BOM。
- 文件名：`投入态势_2025-01_2026-10_20261029.xlsx`。

## 6. 测试

| 测试 | 内容 |
|------|------|
| KpiTest | 总额/项目数/平均分/高风险数/敞口复算 |
| ExposureTest | 预算×分数/100、四舍五入、高风险子集 |
| YoyTest | 有/无上一年、除零 |
| UnanalyzedTest | 未分析项目不计入平均分、单独计数 |
| ScopeTest | GROUP_USER 本组、EXPERT 403 |
| ExportTest | Sheet 数、表头、数值与接口一致 |

## 7. 风险

- 预算为空/0：计入项目数但不产生敞口，页面标记“预算未填”。
- 快照过期：页面显示“分析时间”，提示先运行分析。
- 大额精度：全链路 BigDecimal，前端仅格式化。
- 口径歧义：页面与导出均写明“项目视角、未分析不计入均值”。

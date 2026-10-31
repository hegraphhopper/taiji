# 重复度趋势与归因看板 — 实施计划

## 1. 顺序清单

- [ ] 定义 `TrendQuery`、`TrendPointVO`、`AttributionVO`、`HeatmapVO`。
- [ ] `TrendQueryService`：数据范围、过滤器、快照加载、月份去重、指标聚合。
- [ ] `AttributionService`：项目对加载、视角换算、四类计数、按组矩阵、去重口径。
- [ ] `HeatmapService`：项目组×月份、技术方向×项目组。
- [ ] `TrendSummaryService`：规则摘要与阈值比较。
- [ ] Controller + CSV 导出（BOM、文件名、口径说明）。
- [ ] 后端测试：去重、聚合、归因、权限、空数据、导出。
- [ ] 前端筛选栏 + URL 同步。
- [ ] 趋势折线/柱组合图、归因 100% 堆叠柱、热力图。
- [ ] 下钻抽屉：项目列表 → 项目对对比。
- [ ] 结论摘要面板与空态。
- [ ] 响应式与图表 resize 处理。
- [ ] 演示数据下的视觉与数字核对，截图留档。

## 2. 验证命令

```powershell
mvn -q clean package
mvn test -Dtest='*Trend*,*Attribution*,*Heatmap*'

java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

# 趋势
Invoke-RestMethod "http://localhost:8080/api/v1/insight/trend?startMonth=2025-01&endMonth=2026-10" -Headers $h
# 归因
Invoke-RestMethod "http://localhost:8080/api/v1/insight/attribution?startMonth=2025-01&endMonth=2026-10" -Headers $h
# 热力图
Invoke-RestMethod "http://localhost:8080/api/v1/insight/heatmap?dimension=GROUP_MONTH" -Headers $h
# CSV
Invoke-WebRequest "http://localhost:8080/api/v1/insight/trend/export?startMonth=2025-01&endMonth=2026-10" -Headers $h -OutFile trend.csv

cd frontend
npm run build
npm run dev
```

- 对账：任选一个月，用数据库/接口明细汇总，核对 `analyzedProjectCount`、`highRiskCount`、四类归因计数。
- 权限：`user01` 带 `groupId=2` 请求，结果仍为本组；`expert01` 请求返回 403。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| `TrendQueryService` | 月份去重/口径错误 | 以单元测试与手工对账为准；保留 SQL 明细查询辅助 |
| `AttributionService` | 视角换算错误 | 固定测试夹具覆盖 A/B 双方 |
| 图表组件 | 尺寸/销毁问题 | 统一 ChartContainer；失败时表格兜底 |
| URL 同步 | 循环更新/丢参数 | 单向 watch + replace；测试刷新 |

## 4. 完成定义（DoD）

- 演示数据下 6+ 月趋势、四类归因、两种热力图可展示。
- 趋势、归因、热力图、下钻、导出数字一致。
- 权限与数据范围验证通过；空态与错误态可演示。
- 后端测试与 `npm run build` 通过。
- 页面截图与口径说明纳入交付文档。

## 5. 交接给下游

- 投入态势与决策页复用同一时间/组/方向筛选组件与数据范围逻辑。
- 归因口径说明写入 `docs/metrics.md`，作为评审答疑材料。

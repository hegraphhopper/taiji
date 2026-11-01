# 工作台真实指标 — 实施计划

## 1. 执行顺序

- [x] 1. 后端 DTO：`DashboardSummaryVO`（项目数、已分析数、平均最高分、高风险数、阈值）。
- [x] 2. 后端查询：`DashboardQueryService`，按 `ProjectAccessService.canView` 过滤项目，取每个项目最新快照。
- [x] 3. 后端接口：`GET /api/v1/dashboard/summary`，登录用户可访问。
- [x] 4. 后端测试：PMO 全量、项目组范围、无分析数据、空库。
- [x] 5. 前端类型与 API：`DashboardSummary`、`getDashboardSummary`。
- [x] 6. 前端工作台：三个 KPI 接入真实数据，加载/错误/空态，移除阶段胶囊。
- [x] 7. 验证：`mvn -q test`、`npm run typecheck`、`npm run build`、四角色浏览器走查。
- [x] 8. 更新工作台截图与文档（user-guide/README 如涉及）。

## 2. 指标口径

```text
visibleProjects = 全部项目 filtered by ProjectAccessService.canView
latestSnapshot(project) = 按 analysisDate、runId 取最后一条
analyzed = snapshot != null && snapshot.maxScore != null
avgMaxScore = analyzed 项目 maxScore 的平均值（2 位小数；无数据为 null）
highRiskCount = analyzed 且 maxScore >= thresholds.high 的项目数
```

## 3. 验证命令

```powershell
mvn -q "-Dtest=DashboardSummaryTest" test
mvn -q test
cd frontend; npm.cmd run typecheck; npm.cmd run build; cd ..
```

浏览器：

1. admin/PMO 登录 → 工作台：项目总数、平均最高重复度、高风险项目与接口一致。
2. 项目组用户登录 → 数字仅本组。
3. 专家登录 → 不报错，按可见项目统计。
4. 断网/接口失败 → 显示「—」和提示。

## 4. 风险与回滚

| 风险 | 处理 |
|------|------|
| 快照历史多，取错最新 | 按 analysisDate、runId 升序后取最后一条，与趋势/投入口径一致 |
| 专家可见范围与预期不同 | 复用 `ProjectAccessService.canView`，不另造规则 |
| 接口慢 | 单次查询项目与快照，60/300 项目规模可接受；后续可加缓存 |

回滚：删除新增 dashboard 包与前端调用，恢复 `DashboardView.vue` 占位值。

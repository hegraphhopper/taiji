# 分析运行报告查看 — 前端演示记录

## 1. 本次演示范围

- 分析运行历史新增「查看报告」按钮（仅 SUCCESS 且非 PRECHECK）。
- 弹窗展示项目对列表、等级、归因、重复/差异点数量。
- 点击行展示五维得分、重复点、差异点、模型复核内容。
- 「只看高风险」筛选与分页。
- 数据为前端示例数据，弹窗顶部标注「示例数据」。

## 2. 变更文件

- `frontend/src/types/index.ts`：新增 `RunReportPair`。
- `frontend/src/components/analysis/RunReportDialog.vue`：报告弹窗。
- `frontend/src/views/analysis/AnalysisRunsView.vue`：入口按钮与示例数据。

## 3. 验证

| 项 | 结果 |
|----|------|
| `npm run typecheck` | 退出码 0 |
| `npm run build` | 退出码 0（仅 chunk 体积告警） |
| 弹窗打开 | 8 条示例项目对，按分数倒序 |
| 只看高风险 | 8 → 4 条，总数文案同步 |
| 分页 | 5 条/页，第 2 页 3 条 |
| 点击行 | 五维得分、重复点、差异点、模型复核均显示 |
| 关闭重开 | 筛选、分页、选中项重置 |
| 控制台 | 0 errors；2 条既有 el-radio 弃用警告 |
| 弹窗高度 | 内容区 72vh 内滚动，1280×720 不溢出 |

截图：

- `.trellis/tasks/11-01-fund-run-report/report-demo.png`
- `.trellis/tasks/11-01-fund-run-report/report-demo-detail.png`

## 4. 真实数据接入（2026-11-01）

- 新增 `GET /api/v1/analysis/runs/{id}/pairs`（PMO/ADMIN），直接读取 `analysis_pair`，返回双方摘要、五维分、总分、等级、中性归因（同组/同期/A 在先/B 在先）与重复/差异点。
- 前端移除示例数据与「示例数据」标签，打开弹窗时请求真实接口，增加加载与错误提示。
- 运行 #24 实测返回 19 对：项目名、分数、归因、重复/差异点、模型复核均来自数据库。
- 权限：GROUP_USER/EXPERT 403；缺失运行 404；空运行返回空数组。
- 修复：`V4` 保持历史 70/30，默认值统一由 `V8` 迁移，避免既有库 Flyway checksum 失败。

## 5. 验证

- `mvn -q -Dtest=AnalysisRunPairsTest test`：4/4 通过。
- `mvn -q test`：全量通过。
- `npm run typecheck`、`npm run build`：退出码 0。
- 浏览器：19 对分页、只看高风险、行点击明细、真实项目跳转；控制台 0 errors。

截图：`.trellis/tasks/11-01-fund-run-report/report-real-detail.png`

## 6. 已知限制

- 报告一次返回该运行全部项目对；演示规模（336 对）可接受，若单次运行上万对再考虑服务端分页。
- 不提供导出与跨运行对比。

# 分析运行报告查看 — 实施计划

## 1. 执行顺序

- [x] 1. 类型：`frontend/src/types/index.ts` 增加 `RunReportPair`（项目对 + 分数 + 归因 + 片段计数）。
- [x] 2. 组件：新增 `frontend/src/components/analysis/RunReportDialog.vue`，承载筛选、分页、列表与明细。
- [x] 3. 入口：`AnalysisRunsView.vue` 成功运行行增加「查看报告」，打开弹窗。
- [x] 4. 演示数据：前端内置 8 条示例项目对，弹窗标注「示例数据」。
- [x] 5. 验证：`npm run typecheck`、`npm run build`；本地启动后人工走查。
- [x] 6. 后端新增 `GET /analysis/runs/{id}/pairs`（PMO/ADMIN），返回项目对、分数、中性归因与片段。
- [x] 7. 前端接入真实接口，移除示例数据与「示例数据」标签；加载/错误/空态处理。
- [x] 8. 回归：`AnalysisRunPairsTest` 4 项、`mvn test` 全量、`npm run typecheck/build`。

## 2. 验证命令

```powershell
cd frontend
npm.cmd run typecheck
npm.cmd run build
npm.cmd run dev
```

人工检查：

1. 分析运行页 → 成功运行行 → 查看报告。
2. 列表按分数倒序，高风险标签为红色。
3. 点击行 → 明细出现五维得分、重复点、差异点。
4. 勾选「只看高风险」→ 列表联动；分页正常。
5. 关闭再打开 → 选中项与筛选重置。

## 3. 风险与回滚

| 风险 | 处理 |
|------|------|
| 示例数据被误认为真实结果 | 弹窗顶部固定提示「示例数据」 |
| 弹窗表格行数过多 | 客户端分页，默认 10 条/页 |
| 与后续真实接口字段不一致 | 类型按未来接口形状定义，后续只替换数据源 |

回滚：`git checkout -- frontend/src/views/analysis/AnalysisRunsView.vue` 并删除新增组件/类型。

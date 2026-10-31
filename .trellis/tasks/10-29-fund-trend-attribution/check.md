---
task_id: fund-trend-attribution
result: passed
---
# 检查报告（A4 趋势与归因看板）

## 检查范围

- 后端：`insight/trend` 趋势聚合、月份/粒度去重、项目视角归因、唯一对数、热力图、摘要、下钻、CSV、权限。
- 前端：趋势页、ECharts 三图、项目组归因矩阵、下钻抽屉、URL 同步、菜单角色显隐。
- 演示数据：`demo` profile 历史 seeder、`scripts/backfill-trend.ps1`。
- 依据 `test/用例设计.md` UT-01 ~ UT-20；对应测试类见 `src/test/java/com/taiji/fund/insight/trend/`。

## 命令与结果

| 命令 | 结果 |
|------|------|
| `mvn -q -DskipTests compile` | 退出码 0 |
| `mvn -q test` | 退出码 0；**136 项**通过，Failures 0 / Errors 0 / Skipped 0 |
| `mvn test "-Dtest=TrendPerformanceTest" "-Dtest.excludedGroups=none"` | 1 项通过；60 项目 × 24 月聚合 **133ms**（门禁 500ms） |
| `mvn -q clean package` | 退出码 0 |
| `npm.cmd run typecheck` / `run build` | 退出码 0（TrendView 分包 558.85 kB / gzip 188.04 kB） |

## 运行态验证（demo，H2）

1. 启动自动写入 8 个历史项目（`DEMO-TREND-01..08`，submit_time 回填近 8 个月），重复启动不重复插入。
2. `scripts/backfill-trend.ps1` 逐月 `analysisDate` 运行成功（2026-02 ~ 2026-09），项目数 1→8，项目对 0→4。
3. `GET /insight/trend?startMonth=2026-02&endMonth=2026-09`：
   - 8 个时间点，最后一个点 `proj=8 avg=82.89 highRisk=8 pairCount=8 uniquePairCount=4`
   - 四类归因：`self=2 our=1 peer=1 sim=4`，四类之和 = `pairCount`
   - 环比 +11.66 分；摘要有 4 句且数字与点一致
4. `GET /insight/attribution` 全区间 total=32（self 10 / our 1 / peer 1 / sim 20），按组矩阵 4 行。
5. 热力图：GROUP_MONTH 4 行 × 8 列 = 32 单元；DIRECTION_GROUP 4 行 × 4 列；无数据单元为 null。
6. 下钻：`period=2026-09` 返回 8 个项目，按最高分倒序；浏览器点击趋势点打开抽屉，首行 `多模态知识图谱构建技术研究 / 87.13 / HIGH`。
7. CSV：HTTP 200，`text/csv;charset=UTF-8`，BOM 开头，含口径说明与生成时间。
8. 前端：3 个 canvas 正常渲染；切换粒度后 URL 变为 `granularity=QUARTER` 且 KPI 显示 `2026-Q3`；关闭抽屉后 URL 移除 `period`；控制台 0 error。

## 问题及修复

1. **测试污染**：demo seeder 在 `test` profile 也执行，导致 A3 的 FULL 运行用例 projectCount 变大。修复：`DemoTrendSeedRunner` 显式跳过 `test` profile。
2. **历史回填覆盖当前分**：A3 `analysisDate` 运行会回写 `project.duplicate_score/risk_level`，逐月回填时旧月份会覆盖最新结论。修复：`AnalysisExecutor` 在 `analysisDate != null` 时不回写项目分，并新增 `AnalysisDateBackfillTest`。
3. **下钻点击无效**：ECharts `chart.on('click')` 在混合双轴图上未触发。修复：改用 `zr.on('click')` + `convertFromPixel` 映射最近时间点，并要求点击落在网格内。
4. **URL 回填死循环**：`onBeforeRouteUpdate` 读旧 `route.query`、且每次赋新数组触发 watcher，导致反复 replace/加载。修复：使用 `to.query`，且仅在值变化时赋值。
5. **演示重复度偏低**：种子文本差异过大导致无高风险。调整同主题文本为近重复，形成 4 组撞车（含同组、跨组同期、跨组在先）。

## 未执行项及原因

- MySQL 8 实库验证：本机无 MySQL；A4 未新增迁移，沿用 V4，需在 WeCode/交付环境复验。
- 真实大模型联调：无 Key，A4 不依赖 LLM。
- 正式 60 项目演示数据、截图与视频：归 `10-29-fund-delivery`；A4 的 8 项目历史数据可被其替换。

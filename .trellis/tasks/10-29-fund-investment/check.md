---
task_id: fund-investment
result: passed
---

# 检查报告（B1 技术创新投入态势）

## 检查范围

- 后端：`insight/investment` 概览聚合、四类分布、Top 风险、项目明细、Excel 导出、权限与 URL 参数；`TrendScope` 组范围重载。
- 前端：投入态势页、6 个 KPI、年度/基金/项目组/技术方向/散点图、Top 10 表、明细抽屉、筛选与 URL 同步、导出。
- 数据：demo 基线 60 项目、A3 最新快照与当前阈值配置。
- 依 `test/用例设计.md` UT-I01 ~ UT-I12；测试类位于 `src/test/java/com/taiji/fund/insight/investment/`。

## 命令与结果

| 命令 | 结果 |
|------|------|
| `mvn -q test` | 退出码 0；150 项通过，Failures 0 / Errors 0 / Skipped 0（B1 14 项） |
| `mvn -q -DskipTests package` | 退出码 0；生成 `target/fund-platform-1.0.0.jar` |
| `npm.cmd run build` | 退出码 0（`vue-tsc --noEmit` + Vite，14.56s） |
| `scripts/start-demo.ps1 -SkipBuild` | 后端健康检查通过，前端就绪 |

## 运行态验证（demo，H2）

1. 概览（2024–2026）：40 个项目、51,398,000 元、平均预算 1,284,950 元、已分析 40、未分析 0、平均最高分 54.76、高风险 20、敞口 26,261,380 元、高风险敞口 19,105,867 元、投入同比 -26.8%。
2. 年度分布：2024（8 个/6,121,000）、2025（20 个/26,135,000/均分 63.51）、2026（12 个/19,142,000/均分 65.91）；项目数与金额合计分别等于概览。
3. 基金类型：平台 21/28,276,000、创新 17/21,241,000、其他 2/1,881,000，金额合计等于总额。
4. Top 风险 10 行按敞口倒序；首行 160 万 × 87.98 / 100 = 140.77 万，与接口返回一致。
5. `includeCompleted=false`：项目数 36、结项 0、投入 45,562,000；URL `?includeCompleted=false` 刷新后仍复现。
6. 权限：`user01` 传他组 `groupId=2` 仍只返回本组 6 个项目；`expert01` 403；未登录 401。
7. 导出：Excel HTTP 200，9,462 B，文件头 `PK`，可打开；CSV HTTP 200，`text/csv;charset=UTF-8`，1,278 B，BOM `EFBBBF`，含年度与技术方向两张表，浏览器下载成功。
8. 浏览器：KPI、图表、散点、Top 10 表渲染正常；切换含结项后 URL 更新；控制台 0 error。

## 问题及修复

1. **测试夹具污染共享库**：投资用例自建 12 个项目组后未清理，`ReadOnlyApiTest.ut11` 断言项目组数为 8 时实测 20。修复：`AbstractInvestmentTest` 增加 `@AfterEach` 删除自建快照、项目与项目组；全量回归恢复通过。
2. **含结项开关未同步 URL**：原实现 `@change="loadAll"` 且未进入 watch，切换后数据更新但 URL 不变。修复：移除直接调用，将 `includeCompleted` 纳入 watch，由 `syncQuery + loadAll` 统一处理；浏览器验证 URL 更新且可复现。
3. **状态范围用例缺失**：用例设计 UT-I05 无对应测试。补充 `ut05StatusScopeAndIncludeCompleted`，覆盖默认纳入与 `includeCompleted=false` 排除结项。
4. **CSV 导出缺失**：PRD F6 要求 CSV 至少含年度与技术方向两张表，原实现仅有 Excel。补充 `InvestmentCsvExporter`、`/export/csv` 接口、前端「导出 CSV」按钮与 `ut11bExportCsv` 用例。

## 未执行项及原因

- MySQL 8 实库验证：本机无 MySQL；B1 未新增迁移，需在 WeCode/交付环境复验。
- 正式截图、视频与 docs 全套：归 `10-29-fund-delivery`。
- 导出审计日志：只读导出未写入审计，归 C 阶段治理任务统一实现。

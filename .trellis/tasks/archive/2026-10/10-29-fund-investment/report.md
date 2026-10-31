# 开发报告（B1 技术创新投入态势分析）

## 成果摘要

B1 把项目预算、状态与 A3 的重复度快照放在同一视图，回答「钱投到哪里、投得是否集中、重复投入风险有多大」。后端新增只读聚合模块 `com.taiji.fund.insight.investment`，提供概览 KPI、四类维度分布、Top 风险项目、项目明细与 Excel 导出；前端新增「投入态势」页面，包含 6 个 KPI 卡、年度组合图、基金类型环形图、项目组/技术方向条形图、预算-重复度散点、Top 10 敞口表与项目明细抽屉，筛选条件写入 URL 可复现。

## 已完成需求

- F1 统计口径：仅纳入 `APPROVED/IN_PROGRESS/COMPLETED`；`includeCompleted=false` 排除结项；年份取 `start_date`，缺失回退 `created_at`；重复度取项目最新快照（`analysis_date` 最新，同日取 `runId` 最大）；未分析项目单列且不计入平均分分母。
- F2 概览 KPI：投入总额、项目数、在研/结项数、平均预算、平均最高重复度、高风险项目数、风险敞口、高风险敞口、投入额与项目数同比。
- F3 多维分布：年度（金额/项目数/平均分）、基金类型占比、项目组对比（金额/项目数/敞口/平均分）、技术方向 Top 10、预算-重复度散点。
- F4 风险敞口：敞口 = `预算 × 最高分 / 100`（2 位小数）；高风险按当前配置 `high` 阈值；Top 10 敞口项目表。
- F5 筛选与权限：年度范围、基金类型、项目组、技术方向、含结项；PMO/ADMIN 全量、GROUP_USER 强制本组、EXPERT 403；筛选条件写入 URL。
- F6 导出：POI `SXSSFWorkbook` 生成 Excel，含概览、年度、基金类型、项目组、技术方向、Top 风险与口径说明；另提供 UTF-8 BOM CSV，含年度分布与技术方向分布两张表。
- 前端复用 A4 的图表注册与权限模型；`TrendScope` 增加 `resolve(Long groupId)` 重载供投资模块复用。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn -q test` | 退出码 0，**150/150** 通过（A1–A4 136 + B1 14），Failures/Errors/Skipped 均为 0 |
| `mvn -q -DskipTests package` | 退出码 0，产物 `target/fund-platform-1.0.0.jar` |
| `npm.cmd run build` | 退出码 0（`vue-tsc --noEmit` + Vite）；`InvestmentView` 分包 15,243 B |
| 概览（2024–2026） | 40 个项目、投入 51,398,000 元、平均预算 1,284,950 元、平均最高分 54.76、高风险 20、已分析 40/未分析 0、敞口 26,261,380、高风险敞口 19,105,867、投入同比 -26.8% |
| 年度分布 | 2024：8 个/6,121,000；2025：20 个/26,135,000/均分 63.51/高风险 13；2026：12 个/19,142,000/均分 65.91/高风险 7；合计与概览一致 |
| 基金类型 | 平台 21 个/28,276,000/55.01%；创新 17 个/21,241,000/41.33%；其他 2 个/1,881,000/3.66%；合计等于总额 |
| Top 风险 | 10 行，按敞口倒序；首行「电力设备健康管理平台研发」160 万、87.98、HIGH、140.77 万，与 `预算 × 分数 / 100` 复算一致 |
| 含结项切换 | 关闭后项目数 36、结项 0、投入 45,562,000，URL 变为 `?includeCompleted=false`，刷新可复现 |
| 数据范围 | `user01` 请求他组 `groupId=2` 仍只返回本组（1 组）6 个项目；`expert01` 返回 403；未登录 401 |
| Excel 导出 | HTTP 200，文件 9,462 B，以 `PK` 开头，可正常打开 |
| CSV 导出 | HTTP 200，`text/csv;charset=UTF-8`，1,278 B，BOM `EFBBBF`，含 `[年度分布]` 与 `[技术方向分布]` 两张表；浏览器点击「导出 CSV」成功下载 |
| 浏览器 | 投入态势页 KPI/图表/表格渲染正常，筛选联动，控制台 0 error |

## 关键实现说明

- 聚合为只读：一次加载纳入状态项目，再批量读取最新快照，内存聚合；不新增数据库迁移。
- 最新快照按 `analysis_date, run_id` 升序覆盖写入 Map，最终保留最新一条，避免同日多次运行取错。
- 敞口与平均分统一 `BigDecimal` + `HALF_UP` 2 位；平均分只对已分析项目求和，未分析数单列。
- 同比取筛选结果中的最新年度与上一年度比较；上一年无数据返回 `null`，前端显示「—」。
- 权限复用 `TrendScope`：新增按 `groupId` 解析的重载，GROUP_USER 的服务端覆盖逻辑与 A4 保持一致，前端仅禁用组筛选。
- 导出使用 SXSSF 控制内存，附口径说明，避免中文乱码；CSV 使用 UTF-8 BOM 并转义逗号/引号，兼容 Excel 直接打开。
- 前端筛选统一由 watch 触发 `syncQuery + loadAll`；本次修复了「含结项」开关未进入 watch、URL 不更新的问题。

## 遗留问题与风险

- MySQL 8 实库未验证：本机无 MySQL，B1 未新增迁移，需在交付环境复验。
- GROUP_USER 数据范围沿用单组模型；若后续支持多组用户需扩展 `TrendScope`。
- ECharts 公共分包约 568 kB（gzip 190 kB），后续可进一步拆分。
- 运行态验证基于现有 demo 数据（40 个纳入项目），正式 60 项目演示数据、截图与演示视频由 `10-29-fund-delivery` 统一产出。
- 本次未新增审计写入（只读查询与导出），导出审计需求归 C 阶段治理任务统一处理。

## 相关文档

- `.trellis/tasks/10-29-fund-investment/prd.md`、`design.md`、`implement.md`
- `.trellis/tasks/10-29-fund-investment/test/用例设计.md`
- `.trellis/tasks/10-29-fund-investment/check.md`
- `.trellis/tasks/10-29-fund-investment/test/unit/report-round-1.md`

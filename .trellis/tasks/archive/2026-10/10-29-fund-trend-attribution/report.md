# 开发报告（A4 重复度趋势与归因看板）

## 成果摘要

A4 把 A3 的项目级快照变成可读的趋势账本：按月/季/年聚合平均最高分与高风险项目数，四类归因占比与项目组矩阵解释"问题来自谁"，两种热力图定位"集中在哪个组/方向"，趋势点可下钻到项目与项目对，CSV 可导出复用。口径定为**项目视角 + 单列唯一对数**、**高风险按落库判定**，与项目详情页数字一致。

## 已完成需求

- F1 趋势指标：分析项目数、平均最高分/平均 Top-K、高风险项目数、项目视角对数、唯一对数、高风险对数、环比；同项目同粒度只取最新运行。
- F2 归因：四类计数与占比、按组矩阵；`pairCount` 等于四类之和。
- F3 热力图：项目组×月份、技术方向×项目组，平均分/高风险数切换，样本数 tooltip。
- F4 下钻：趋势点 → 项目列表（等级可选）→ 项目详情；`period` 写入 URL 可复现。
- F5 摘要：规则化 3–5 句，数字与图表同源；空数据提示。
- F6 权限：PMO/ADMIN 全量、GROUP_USER 强制本组、EXPERT 菜单隐藏 + 403。
- 导出：UTF-8 BOM CSV + 口径说明 + 生成时间。
- 演示：demo 历史 seeder + 逐月回填脚本，生成 8 个月真实快照。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn -q test` | 136/136 通过（A1–A3 113 + A4 23） |
| 性能用例 | 60 项目 × 24 月 133ms |
| `mvn -q clean package` | 通过 |
| `npm.cmd run typecheck` / `run build` | 通过 |
| 8 个月趋势 | 8 点，末点 82.89 分、8 个高风险、8 次视角对 / 4 个唯一对 |
| 四类归因 | self=2 / our=1 / peer=1 / sim=4，合计=pairCount |
| 热力图 | 4×8 与 4×4，单元格样本数正确 |
| 下钻与 URL | 抽屉 8 行、等级过滤、刷新可复现、关闭清理 |
| CSV | BOM + 口径 + 两行数据，列与页面一致 |
| 浏览器 | 3 图渲染、粒度/维度切换、0 console error |

## 关键实现说明

- 去重键为 `(projectId, period)` 取最大 `runId`；period 由 `analysis_date` 直接计算，避免时区漂移。
- 归因直接汇总快照四类计数，跨组对双方各计一次；唯一对数由选定 run 的 `analysis_pair` 无序去重，因此 `uniquePairCount ≤ pairCount`。
- 高风险数使用 `high_pair_count > 0`，改阈值不影响历史；下钻的等级筛选按当前阈值折算并在抽屉内注明。
- 项目组矩阵与热力图复用同一套过滤/去重逻辑，三者样本集合一致。
- 前端 ECharts 按需注册（line/bar/heatmap + grid/tooltip/legend/visualMap），独立分包，路由懒加载。
- 下钻点击基于 zrender 坐标映射，避免混合双轴图序列点击失效；URL 同步使用目标路由并做值比较，避免循环。

## 遗留问题与风险

- 趋势顶部不提供 level 筛选（与"按当时阈值"冲突），等级过滤仅在下钻；若评审要求顶部可筛，需要改为重算口径并同步说明。
- GROUP_USER 的组筛选由服务端覆盖，前端仅禁用；若未来支持多组用户，需要扩展数据范围模型。
- 应用层聚合在 60 项目 × 24 月为 133ms；超过 5 万行快照时需下沉 SQL 或预聚合。
- ECharts 使趋势页分包增至 559 kB（gzip 188 kB），后续可再拆分 heatmap。
- 本机无 MySQL，V4 与 A4 查询需在交付环境复验。
- 演示数据为 8 个历史项目，D 阶段将替换为 60 项目正式演示集并重跑回填。

## 相关文档

- `.trellis/tasks/10-29-fund-trend-attribution/prd.md`、`design.md`、`implement.md`
- `.trellis/tasks/10-29-fund-trend-attribution/test/用例设计.md`
- `.trellis/tasks/10-29-fund-trend-attribution/check.md`
- `.trellis/spec/backend/insight-guidelines.md`

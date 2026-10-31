# 开发报告（B2 新项目辅助决策与报告导出）

## 成果摘要

B2 把「申报前预检」做成可执行结论：复用 A3 预检得到 Top-5 相似项目，叠加可配置规则引擎输出建议动作，计算预计重复投入与建议评审专家，并把结果保存为不可编辑的决策记录，最终导出 Word 决策报告与 Excel 明细。

- 后端新增 `com.taiji.fund.decision`：规则、预检、专家建议、决策记录、Word/Excel 报告。
- 数据库新增 Flyway `V5__decision_schema.sql`：`decision_rule`、`decision_record` 与 5 条默认规则。
- 前端新增 `/decision`（预检）、`/decision/records`（记录与下载）、`/decision/rules`（管理员规则配置），菜单与路由按角色显隐。

## 已完成需求

- F1 预检入口：`PROJECT` 复用草稿/已提交项目内容；`TEXT` 临时文本预检（不落项目、只存 SHA-256 摘要）；预检不修改项目状态与分数。
- F2 预检结果：综合风险分、等级、Top-5 相似项目、分维度分、重复/差异点、建议动作、预计重复投入、建议专家、理由与 `llmUsed`。
- F3 规则配置：默认 5 条规则覆盖 0–100；优先级升序首条命中；未命中回退 EXPLAIN；管理员整体替换规则，保存后立即生效并写审计。
- F4 决策记录：保存 Top-5 快照、列表筛选、详情回看、逻辑作废；记录不可编辑。
- F5 报告导出：Word（标题、元信息、结论、相似项目、重复点、差异点、口径）与 Excel（相似项目、重复点、差异点、口径四个 Sheet），文件名含项目编码与日期。
- F6 权限：组用户可预检本组并查看本组记录；专家只读查看同组非草稿记录、不可保存；PMO/ADMIN 全量；导出沿用记录访问控制并写审计。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn -q test` | 退出码 0，**158/158** 通过（B2 8 项），Failures/Errors/Skipped 均为 0 |
| `npm.cmd run typecheck` / `run build` | 退出码 0；新增页面独立分包，控制台 0 error |
| 默认规则 | 59.99→APPROVE；60/79.99→EXPLAIN；80 同组/他组在先→MERGE；80 我方在先→EXPLAIN；90/100→TERMINATE |
| PROJECT 预检（demo） | DEMO-002：风险分 91.60、HIGH、TERMINATE、Top-1 为 DEMO-001、重复点 3 条、金额 82.44 万、专家「评审专家」、`llmUsed=false` |
| 保存与查询 | 记录保存成功，列表 total=1、详情快照完整；组外用户 403；专家可读；组用户作废 403、PMO 作废后列表归零 |
| Word 报告 | 3,551 B，`PK` 头；POI 回读含标题、决策结论、Top-5、重复点、差异点、口径；文件名 `决策报告_DEMO-002_20261031.docx` |
| Excel 报告 | 7,097 B，`PK` 头；四个 Sheet 齐全，浏览器下载成功 |
| 规则接口 | 返回 5 条默认规则，字段与优先级正确 |

## 关键实现说明

- 规则匹配：`score >= min` 且（`score < max` 或 `max >= 100`）；支持 `relation` 与 `same_group` 条件；同组/他组在先的 MERGE 拆为两条规则，避免 OR 条件建模。
- 预检不落库：直接调用 `AnalysisRunService.precheck`，`analysis_run` 记录 `PRECHECK` 运行；不写快照与项目分数。
- 建议专家：系统没有用户—技术方向直连字段，采用「历史评审同方向优先 + 进行中评审数升序」；无专家回退 PMO，再回退 ADMIN，最多 3 人，不返回联系方式。
- 决策记录：保存客户端回传的 Top-5 快照（≤60KB、JSON 校验），记录只作废不物理删除；列表按角色数据范围过滤后分页。
- Word 使用 `XWPFDocument` 并显式设置宋体 `eastAsia`；Excel 使用 `SXSSFWorkbook` 控制内存；两者均从记录快照生成，避免实时数据漂移。
- 导出动作写 `DECISION_REPORT_EXPORT` 审计，规则变更写 `DECISION_RULE_UPDATE` 审计。

## 遗留问题与风险

- MySQL 8 实库未验证：V5 仅使用 H2/MySQL 兼容语法，需在交付环境复验。
- 记录快照由前端提交，服务端仅校验 JSON 合法性；若需防篡改，可改为服务端按 runId 重放预检。
- 引擎预检不调用大模型，B2 结论固定为本地规则；后续若需大模型理由，可在 `AnalysisExecutor.precheck` 增加候选对语义复核。
- 专家方向匹配依赖历史评审数据；全新环境无评审记录时回退为负载排序。
- 报告字体在缺少宋体的环境可能回退，需在交付环境抽查。
- 规则保存采用「删后重插」，并发保存时后写覆盖先写；演示场景可接受。

## 相关文档

- `.trellis/tasks/10-29-fund-decision/prd.md`、`design.md`、`implement.md`
- `.trellis/tasks/10-29-fund-decision/test/用例设计.md`
- `.trellis/tasks/10-29-fund-decision/test/unit/report-round-1.md`
- `.trellis/tasks/10-29-fund-decision/check.md`

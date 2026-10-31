# 单元测试报告 Round 1

## 执行信息

- 执行命令：`mvn -q test`（仓库根目录）
- 用例总数：14 ｜通过：14 ｜失败：0 ｜跳过：0
- 通过率：100%｜阈值：100%｜轮次：1/5
- 全量套件：53 个测试类 / 172 项，Failures 0 / Errors 0 / Skipped 0；Maven 退出码 0
- 测试环境：`@ActiveProfiles({"demo","test"})` + H2 内存库 + MockMvc
- 前端验证：`npm.cmd run typecheck` 退出码 0；`npm.cmd run build` 退出码 0（仅 chunk 体积告警）

## C1 用例执行明细

| 用例ID | 测试方法 | 测试类 | 优先级 | 结果 |
|--------|----------|--------|--------|------|
| UT-G01 | ut01DefaultAndUpdate | SystemConfigTest | P0 | 通过 |
| UT-G02 | ut02PermissionAndValidation | SystemConfigTest | P0 | 通过 |
| UT-G03 | ut03CreateResetDisable | UserAdminTest | P0 | 通过 |
| UT-G04 | ut04PermissionAndDeleteGuards | UserAdminTest | P0 | 通过 |
| UT-G05 | ut05ListAndRoles | UserAdminTest | P1 | 通过 |
| UT-G06 | ut06PassGateAndFullFlow | ApprovalFlowTest | P0 | 通过 |
| UT-G07 | ut07RejectAndNewRound | ApprovalFlowTest | P0 | 通过 |
| UT-G08 | ut08IllegalAndTerminate | ApprovalFlowTest | P0 | 通过 |
| UT-G09 | ut09ReviewOnceAndTodo | ReviewTodoTest | P1 | 通过 |
| UT-G10 | ut10NewRoundAndDraftScope | ReviewTodoTest | P1 | 通过 |
| UT-G11 | ut11QueryExportAndMasking | AuditQueryTest | P0 | 通过 |
| UT-G12 | ut12FilterByResourceAndResult | AuditQueryTest | P1 | 通过 |
| UT-G13 | ut13EndpointMatrix | PermissionMatrixTest | P0 | 通过 |
| UT-G14 | ut14GroupScopeAndExpertReviewing | PermissionMatrixTest | P0 | 通过 |

## 本轮修复（check 发现的需求偏差）

1. **PASS 门禁口径**：PRD 澄清「同一专家多轮 PASS 只计 1 人」，原实现只统计当前轮次，驳回重提后旧 PASS 清零。改为按项目全部轮次统计不同专家数；`UT-G07` 由「新轮 PASS=0」改为「新轮 PASS=1（旧 PASS 计入）」，并保留 1 票不能立项、A 重复 PASS 不增量、B 通过后达 2 票的断言。
2. **PMO 越权项目维护**：PRD F4 将提交/撤回/开始执行/结项划归项目组，原实现因 `isUnrestricted()` 允许 PMO。`ProjectAccessService` 改为仅 ADMIN 或同组 GROUP_USER 可创建/编辑；前端 `canManage`/`canCreate` 与权限目录同步收敛；`UT-G14` 新增 PMO 创建、提交、开始均 403 的断言。

## 失败用例明细

无（14/14 通过）。

## 剩余失败

无。

## 证据与说明

- `target/surefire-reports/`：C1 六个测试类 14 项全部通过；全量 172 项零失败。
- `mvn -q -DskipTests package` 退出码 0，产出 `target/fund-platform-1.0.0.jar`。
- 未使用 skip/only/@Disabled，未删除或放宽断言；未执行 `git commit`。

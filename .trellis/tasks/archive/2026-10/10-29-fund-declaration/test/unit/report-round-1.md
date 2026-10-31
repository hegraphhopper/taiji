# 单元测试报告 Round 1

## 执行信息

- 执行命令：`mvn -q test`（仓库根目录）
- 用例总数：24｜通过：24｜失败：0｜跳过：0
- 通过率：100%｜阈值：100%｜轮次：1/5
- 全量套件：46 项（A1 基线 22 项 + A2 新增 24 项）全部通过，Maven 退出码 0
- 测试环境：`@ActiveProfiles({"demo","test"})` + H2 内存库 + MockMvc；附件写入 `target/test-storage`，未触碰生产 `storage/`

## A2 用例执行明细

| 用例ID | 测试方法 | 测试类 | 优先级 | 结果 |
|--------|----------|--------|--------|------|
| UT-01 | ut01_createDraftGeneratesCode | ProjectServiceTest | P0 | 通过 |
| UT-02 | ut02_duplicateManualCodeRejected | ProjectServiceTest | P0 | 通过 |
| UT-03 | ut03_budgetAndDateValidation | ProjectServiceTest | P1 | 通过 |
| UT-22 | ut22_optimisticLockRejectsStaleUpdate | ProjectServiceTest | P2 | 通过 |
| UT-23 | ut23_manualAndAutoCodesCoexist | ProjectServiceTest | P1 | 通过 |
| UT-04 | ut04_saveSectionsReplacesAndOrders | ProjectSectionServiceTest | P0 | 通过 |
| UT-05 | ut05_sectionSaveRollsBackOnFailure | ProjectSectionServiceTest | P1 | 通过 |
| UT-06 | ut06_submitRequiresCompleteSections | ProjectWorkflowServiceTest | P0 | 通过 |
| UT-07 | ut07_submitWritesSubmitTimeAndLog | ProjectWorkflowServiceTest | P0 | 通过 |
| UT-08 | ut08_withdrawAndResubmitKeepsFirstSubmitTime | ProjectWorkflowServiceTest | P1 | 通过 |
| UT-09 | ut09_approveAndReject | ProjectWorkflowServiceTest | P0 | 通过 |
| UT-10 | ut10_illegalTransitionRejected | ProjectWorkflowServiceTest | P0 | 通过 |
| UT-11 | ut11_approverRoleEnforced | ProjectWorkflowServiceTest | P0 | 通过 |
| UT-24 | ut24_approvalLogsAscendingAndAccessControlled | ProjectWorkflowServiceTest | P1 | 通过 |
| UT-12 | ut12_crossGroupAccessDenied | ProjectQueryServiceTest | P0 | 通过 |
| UT-13 | ut13_listFiltersSortsPaginates | ProjectQueryServiceTest | P1 | 通过 |
| UT-14 | ut14_deleteDraftOnly | ProjectDeleteAndAuditTest | P1 | 通过 |
| UT-15 | ut15_writeOperationsAudited | ProjectDeleteAndAuditTest | P0 | 通过 |
| UT-16 | ut16_docxUploadParsesSections | ProjectDocumentApiTest | P0 | 通过 |
| UT-17 | ut17_pdfTxtMdParsed | ProjectDocumentApiTest | P0 | 通过 |
| UT-18 | ut18_invalidTypeSizeAndCountRejected | ProjectDocumentApiTest | P0 | 通过 |
| UT-19 | ut19_parseFailureAndRetry | ProjectDocumentApiTest | P1 | 通过 |
| UT-20 | ut20_downloadAndPathSafety | ProjectDocumentApiTest | P1 | 通过 |
| UT-21 | ut21_longTextTruncated | ProjectDocumentApiTest | P2 | 通过 |

## 失败用例明细

首轮最终运行无失败用例（0 项）。

## 修复说明

- `src/test/java/com/taiji/fund/project/ProjectQueryServiceTest.java:108`：预算区间查询原先使用默认 `updatedAt desc` 排序，却按预算倒序断言。为区间查询补充显式 `sortBy=budget&sortOrder=desc`，使断言与查询契约一致；未删除断言、未放宽期望、未修改生产代码。
- 其余用例首轮即通过，无测试意图调整。

## 剩余失败

无。

## 证据与说明

- `target/surefire-reports/` 各类退出统计：ProjectServiceTest 5、ProjectSectionServiceTest 2、ProjectWorkflowServiceTest 7、ProjectQueryServiceTest 2、ProjectDeleteAndAuditTest 2、ProjectDocumentApiTest 6，Failures/Errors/Skipped 均为 0。
- 未使用 `it.skip` / `describe.skip` / `.only` / `@Disabled`，无调试输出残留。
- 未发现需要交回修复轮的生产代码缺陷；未执行 `git commit`。

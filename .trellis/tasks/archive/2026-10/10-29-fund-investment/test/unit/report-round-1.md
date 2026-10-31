# 单元测试报告 Round 1

## 执行信息

- 执行命令：`mvn -q test`（仓库根目录）
- 用例总数：12｜通过：12｜失败：0｜跳过：0
- 测试方法：14（UT-I10 拆为「组用户强制本组」与「专家 403 / 匿名 401」；UT-I11 拆为 Excel 与 CSV）
- 通过率：100%｜阈值：100%｜轮次：1/5
- 全量套件：150 项（A1–A4 基线 136 项 + B1 新增 14 项）全部通过，Maven 退出码 0
- 测试环境：`@ActiveProfiles({"demo","test"})` + H2 内存库 + MockMvc；项目/快照由 Mapper 直接构造，测试结束清理自建项目组与项目

## B1 用例执行明细

| 用例ID | 测试方法 | 测试类 | 优先级 | 结果 |
|--------|----------|--------|--------|------|
| UT-I01 | ut01OverviewKpi | InvestmentOverviewTest | P0 | 通过 |
| UT-I02 | ut02ExposureAndHighExposure | InvestmentOverviewTest | P0 | 通过 |
| UT-I03 | ut03UnanalyzedExcludedFromAverage | InvestmentOverviewTest | P0 | 通过 |
| UT-I04 | ut04YearUsesStartDateWithCreatedAtFallback | InvestmentDistributionTest | P0 | 通过 |
| UT-I05 | ut05StatusScopeAndIncludeCompleted | InvestmentOverviewTest | P0 | 通过 |
| UT-I06 | ut06YoyBudgetAndCount | InvestmentDistributionTest | P1 | 通过 |
| UT-I07 | ut07FundTypeAndDirectionDistribution | InvestmentDistributionTest | P1 | 通过 |
| UT-I08 | ut08TopRiskOrderedByExposure | InvestmentDrillAndExportTest | P1 | 通过 |
| UT-I09 | ut09DrillProjectsAndUnanalyzedFlag | InvestmentDrillAndExportTest | P1 | 通过 |
| UT-I10 | ut10GroupUserForcedToOwnGroup | InvestmentScopeTest | P0 | 通过 |
| UT-I10 | ut10bExpertForbiddenAndAnonymousUnauthorized | InvestmentScopeTest | P0 | 通过 |
| UT-I11 | ut11ExportXlsx | InvestmentDrillAndExportTest | P1 | 通过 |
| UT-I11 | ut11bExportCsv | InvestmentDrillAndExportTest | P1 | 通过 |
| UT-I12 | ut12EmptyRange | InvestmentOverviewTest | P1 | 通过 |

## 失败用例明细

首轮最终运行无失败用例（0 项）。

## 修复说明

- 新增 UT-I05：覆盖「仅纳入 APPROVED/IN_PROGRESS/COMPLETED」与 `includeCompleted=false` 排除结项，补齐用例设计中的状态范围用例。
- `AbstractInvestmentTest` 增加 `@AfterEach` 清理：删除自建快照、项目与项目组。原因：投资用例自建项目组后未清理，污染共享 H2 上下文，导致 `ReadOnlyApiTest.ut11` 断言项目组数为 8 时失败（实测 20）；修复后全量回归通过。未放宽或删除既有断言，未修改生产代码。
- 补充 CSV 导出用例 `ut11bExportCsv`，校验 BOM、`text/csv` 类型与年度/技术方向两张表。

## 剩余失败

无。

## 证据与说明

- `target/surefire-reports/`：44 个测试类、150 项，Failures 0 / Errors 0 / Skipped 0。
- B1 分布：`InvestmentOverviewTest` 5、`InvestmentDistributionTest` 3、`InvestmentDrillAndExportTest` 4、`InvestmentScopeTest` 2。
- 未使用跳过或仅执行标记，无调试输出残留。
- 未执行 `git commit`。

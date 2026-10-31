# 单元测试报告 Round 1

## 执行信息

- 执行命令：`mvn -q test`（仓库根目录）
- 用例总数：9｜通过：9｜失败：0｜跳过：0
- 测试方法：8（UT-D09 与 UT-D03 合并断言）
- 通过率：100%｜阈值：100%｜轮次：1/5
- 全量套件：158 项（A1–B1 基线 150 项 + B2 新增 8 项）全部通过，Maven 退出码 0
- 测试环境：`@ActiveProfiles({"demo","test"})` + H2 内存库 + MockMvc；项目与评审记录由 Mapper 直接构造，文本使用随机唯一标记避免跨用例干扰

## B2 用例执行明细

| 用例ID | 测试方法 | 测试类 | 优先级 | 结果 |
|--------|----------|--------|--------|------|
| UT-D01 | ut01DefaultRuleBoundaries | DecisionRuleTest | P0 | 通过 |
| UT-D02 | ut02SaveAndValidateRules | DecisionRuleTest | P1 | 通过 |
| UT-D03 | ut03ProjectPrecheckHighDuplicate | DecisionPrecheckTest | P0 | 通过 |
| UT-D04 | ut04TextPrecheckHashAndOptionalBudget | DecisionPrecheckTest | P1 | 通过 |
| UT-D05 | ut05SuggestedExpertsPreferSameDirection | DecisionPrecheckTest | P1 | 通过 |
| UT-D06 | ut06RecordLifecycleAndScope | DecisionRecordReportTest | P0 | 通过 |
| UT-D07 | ut07WordAndExcelReports | DecisionRecordReportTest | P0 | 通过 |
| UT-D08 | ut08Permissions | DecisionPrecheckTest | P0 | 通过 |
| UT-D09 | ut03ProjectPrecheckHighDuplicate（降级断言） | DecisionPrecheckTest | P1 | 通过 |

## 失败用例明细

首轮最终运行无失败用例（0 项）。

## 修复说明

- UT-D01 首轮将 89.99 分同组预期误写为 EXPLAIN，实际命中 80–90 同组 MERGE；修正断言而非修改规则。
- UT-D02 首轮构造了 priority=0 的非法规则，先触发优先级校验；调整测试夹具后分别覆盖动作非法、区间倒置、区间覆盖不完整。
- 无生产代码缺陷需要修复；未删除或放宽断言。

## 剩余失败

无。

## 证据与说明

- `target/surefire-reports/`：47 个测试类、158 项，Failures 0 / Errors 0 / Skipped 0。
- B2 分布：`DecisionRuleTest` 2、`DecisionPrecheckTest` 4、`DecisionRecordReportTest` 2。
- 未使用跳过或仅执行标记，无调试输出残留。
- 未执行 `git commit`。

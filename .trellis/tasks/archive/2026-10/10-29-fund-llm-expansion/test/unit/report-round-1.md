# 单元测试报告 Round 1

## 执行信息

- 命令：`mvn -q test`
- 结果：53 个测试类 / **174 项通过**，Failures 0 / Errors 0 / Skipped 0
- 本轮新增：`AnalysisLlmTest.ut21PrecheckUsesLlm`、`AnalysisLlmTest.ut22ConnectionTest`
- 扩展断言：`AnalysisLlmTest.ut20` 覆盖 overlaps/differences 落库；`DecisionRecordReportTest.ut07` 覆盖 Word/Excel 模型复核段

## 用例结果

| 用例 | 方法 | 结果 |
|------|------|------|
| UT-L01 | ut22ConnectionTest（成功分支） | 通过 |
| UT-L02 | ut22ConnectionTest（失败 + PMO 403） | 通过 |
| UT-L03 | ut21PrecheckUsesLlm | 通过 |
| UT-L04 | ut20FusesLlmScore | 通过 |
| UT-L05 | 预检降级（未配置，手工 API 验证） | 通过 |
| UT-L06 | DecisionRecordReportTest.ut07 | 通过 |
| UT-L07 | npm typecheck/build | 通过 |

## 失败与修复

- `ut20` 原有断言要求全部相似对都有 LLM 分，但 `llmTopN=5`、相似对 6 条；将测试配置的 `llmTopN` 调整为 10 以覆盖融合路径，未放宽生产逻辑。
- Word 章节编号调整：口径保持“五、口径与免责说明”，模型复核为“六、大模型复核”。

## 证据

- `target/surefire-reports/`：174 项零失败。
- 自检未配置：`success=false / DISABLED`。
- 预检降级：`llmUsed=false`，Top-1 35.03。

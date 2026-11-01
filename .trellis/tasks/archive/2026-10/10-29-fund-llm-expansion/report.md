# 大模型能力扩展 — 验收报告

## 1. 交付内容

- 连接自检：`POST /api/v1/system/llm/test`（ADMIN），返回 `success/status/latency/model/message`，不含 Key；分析配置页新增“测试连接”。
- 决策预检：`AnalysisExecutor.precheck` 对 Top-1 相似对调用模型，返回 `llmUsed/llmReason`；风险分与规则建议不变；失败自动降级。
- 重复/差异点：V7 新增 `analysis_pair.llm_overlaps`、`llm_differences`；`PeerMatchVO` / `PairDetailVO` 贯通；项目详情展示模型重复点/差异点。
- 报告：Word 新增“六、大模型复核”；Excel 新增“模型复核”Sheet；无模型数据时输出“本地算法结果”。
- 前端：分析配置测试连接、项目详情模型复核点、决策预检模型理由。

## 2. 验证结果

| 项 | 结果 |
|----|------|
| 全量测试 | 53 个测试类 / **174 项通过**，Failures 0 / Errors 0 / Skipped 0 |
| 新增用例 | `ut21PrecheckUsesLlm`、`ut22ConnectionTest`；报告断言扩展 |
| 后端构建 | `mvn -q -DskipTests package` 退出码 0 |
| 前端类型检查 | `npm run typecheck` 退出码 0 |
| 前端构建 | `npm run build` 退出码 0（仅 chunk 体积告警） |
| 自检（未配置） | 返回 `success=false / DISABLED`，提示未配置 `LLM_BASE_URL` |
| 预检降级 | `llmUsed=false`，Top-1 总分 35.03，规则建议正常 |

## 3. 说明

- 当前环境未注入 `LLM_BASE_URL/LLM_API_KEY/LLM_MODEL`，因此真实模型调用未联调；代码路径、失败降级与 mock 成功路径均已验证。
- 配置真实模型后，按“解释层”模式设置 `localWeight=100 / llmWeight=0`，总分与趋势不变。
- 未执行 `git push`：仓库未配置 remote。

## 4. 真实模型联调补充（2026-11-01）

- `application-local.yml`（根目录、gitignore）配置 DeepSeek：`https://api.deepseek.com` / `deepseek-flash`。
- JDK 8 需 `-Djavax.net.ssl.trustStoreType=Windows-ROOT`；已写入 `scripts/start-demo.ps1`。
- 自检：成功，985 ms。
- FULL 分析：`llmUsed=true`，56 项目 / 19 对 / 20.8 s；项目 #2 总分 91.60 与本地一致，模型分 90，理由/重复点/差异点入库。
- 预检 #42：`llmUsed=true`，风险分 35.03 不变，模型分 76。
- 日志：8 次，7 成功，平均 3393 ms。
- Word/Excel 报告已含模型复核段。

# 大模型能力扩展 — 技术设计

## 1. 复用

- `LlmClient`、`LlmPromptBuilder`、`LlmProperties`、`LlmResponseParser`、`LlmCache`、`LlmCallLog` 已存在。
- `AnalysisExecutor.applyLlm` 已实现分析融合与失败降级。
- `DecisionWordReportBuilder` / `DecisionExcelReportBuilder` 已存在。

## 2. 数据

V7：

```sql
ALTER TABLE analysis_pair ADD COLUMN llm_overlaps TEXT;
ALTER TABLE analysis_pair ADD COLUMN llm_differences TEXT;
```

`AnalysisPair` 增加同名字段；`PeerMatchVO` / `PairDetailVO` 增加 `List<String> llmOverlaps/llmDifferences`。

## 3. 接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/system/llm/test` | ADMIN | 固定 prompt 真实调用，返回 status/latency/model/message，不返回 Key |
| POST | `/api/v1/projects/{id}/...` | 既有 | 预检路径由 `AnalysisExecutor.precheck` 接入 Top-1 LLM |
| GET | `/api/v1/analysis/llm-logs` | ADMIN | 可选：调用日志分页查询 |

## 4. 流程

- 分析：`applyLlm` 成功后同时写 `llmScore/llmReason/llmOverlaps/llmDifferences`。
- 预检：`AnalysisExecutor.precheck` 对 Top-1 草稿调用 LLM，写回实体；`AnalysisRunService` 更新 `llmUsed`；`DecisionPrecheckService` 透传。
- 报告：从 `DecisionRecordVO.similarProjects` 读取模型字段；无数据时输出“本地算法结果”。

## 5. 降级

- 未配置、超时、HTTP 错误、JSON 非法：`llmUsed=false`，沿用本地分与本地理由。
- 预检超时上限 5000ms，不影响风险分。

## 6. 测试

- 自检成功/失败、权限、无 Key 泄露。
- 预检开启/关闭、失败降级、风险分不变。
- V7 字段读写与 VO 映射。
- 报告含“大模型复核”与降级文案。

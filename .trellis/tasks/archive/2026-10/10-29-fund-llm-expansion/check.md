# 大模型能力扩展 — 检查结论

| 检查项 | 结论 | 证据 |
|--------|------|------|
| 密钥安全 | 通过 | 自检响应无 `apiKey`；Key 仅来自环境变量；日志不记录 prompt/Key |
| 权限 | 通过 | `/system/llm/test` 仅 ADMIN；PMO 403（UT-L02） |
| 预检不改分 | 通过 | `totalScore` 与 `localScore` 一致；风险分/建议不变（UT-L03） |
| 降级 | 通过 | 未配置返回 DISABLED；预检 `llmUsed=false` 且正常返回 |
| 数据迁移 | 通过 | V7 在 H2 全量测试中执行成功；字段读写与 VO 映射通过 |
| 报告 | 通过 | Word 含“六、大模型复核”，Excel 含“模型复核”Sheet；降级文案存在 |
| 前端 | 通过 | typecheck/build 退出码 0 |
| 回归 | 通过 | 174/174 |

## 遗留

- 真实模型联调：需注入 `LLM_BASE_URL/LLM_API_KEY/LLM_MODEL` 后执行一次成功调用。
- 调用日志页面未实现（API/表已具备），不在本轮核心范围。

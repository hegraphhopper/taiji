# 重复度分析引擎 — 实施计划

## 1. 顺序清单

- [ ] Flyway `V4__analysis_schema.sql` + 默认配置种子；H2 启动验证。
- [ ] 配置实体/服务/接口：校验、版本、审计。
- [ ] `TextNormalizer`、`StopWords`、`TermDictionary`、`ChineseTokenizer`。
- [ ] `TfIdfVectorizer`、`SparseVector`、`CosineSimilarity`、`JaccardSimilarity`。
- [ ] `ProjectAnalysisInput` 构建（结构化字段 + 附件文本回退 + 标签）。
- [ ] `DimensionScorer`（5 维度）、`ScoreFusion`（权重归一化）、`DuplicateLevel`。
- [ ] `AttributionResolver`（同组/在先/同期，落 `earlier_project_id`）。
- [ ] `MatchSnippetExtractor`（Top 词 + 原文片段 + 差异点）。
- [ ] `LlmClient`（RestTemplate）、Prompt、响应校验、缓存、熔断、`llm_call_log`。
- [ ] `AnalysisExecutor`：全量/单项目/预检流程、并发保护、事务、失败清理。
- [ ] 快照聚合与 `project.duplicate_score/risk_level` 回写。
- [ ] 查询服务与接口：报告、Top-K、项目对、运行历史。
- [ ] 单元测试夹具（相同/无关/部分重叠/跨组时间边界）与降级测试。
- [ ] 性能测试：300 项目夹具计时并记录。
- [ ] 分析配置页面（管理员）与分析运行/报告页面（前端最小闭环）。

## 2. 验证命令

```powershell
mvn -q clean package
mvn test -Dtest='*Analysis*,*Similarity*,*Tokenizer*,*Attribution*,*Llm*'

java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

# 触发全量分析
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/analysis/runs `
  -Headers $h -ContentType 'application/json' -Body '{"triggerType":"FULL"}'

# 项目报告
Invoke-RestMethod http://localhost:8080/api/v1/analysis/projects/1 -Headers $h

cd frontend
npm run build
```

- 校验降级：设置 `LLM_ENABLED=true` + 错误 `LLM_BASE_URL`，运行后检查 `llmUsed=false` 与 `llm_call_log`。
- 校验权重：将 content 权重改为 100、其余 0，重跑后总分应等于 content 维度分（±0.01）。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| `V4__analysis_schema.sql` | 方言/索引问题 | 演示库重建；只增不改 |
| `ChineseTokenizer` | 分词效果差 | 调整 n-gram/术语表；夹具回归 |
| `LlmClient` | 阻塞/超时拖慢运行 | 默认关闭、单次超时、并发 2、熔断 |
| `AnalysisExecutor` | 部分写入 | 事务 + 失败清理；run 状态可重试 |
| 快照聚合 | 与明细不一致 | 以 pair 为事实源重建；一致性测试 |

## 4. 完成定义（DoD）

- 夹具测试全部通过，分数与等级符合 PRD 验收区间。
- 权重、归因、同期边界、降级、缓存、快照一致性均有测试。
- 300 项目本地分析 ≤5s，记录实际耗时。
- 项目报告接口可返回 Top-K 与片段，可供趋势/决策直接复用。
- 默认配置与种子数据随 Flyway 初始化，管理员页面可改并生效。
- 无密钥、无 prompt 正文、无用户敏感信息进入日志。

## 5. 交接给下游

- 趋势：读取 `duplicate_snapshot` 与 `analysis_pair`，字段与口径见父设计 5.4。
- 投入：读取 `duplicate_snapshot.max_score` 与 `project.budget` 计算敞口。
- 决策：复用 `AnalysisExecutor` 的预检能力与 `analysis_pair` 结果，不重复实现相似度。
- 建议：所有下游只读分析结果，不直接调用分词/相似度工具，避免口径分叉。

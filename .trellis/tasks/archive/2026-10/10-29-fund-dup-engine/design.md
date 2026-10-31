# 重复度分析引擎 — 技术设计

## 1. 模块结构

```text
analysis/
  config/     AnalysisConfigEntity, AnalysisConfigService, AnalysisConfigController
  tokenizer/  TextNormalizer, ChineseTokenizer, StopWords, TermDictionary
  vector/     TfIdfVectorizer, SparseVector, CosineSimilarity, JaccardSimilarity
  score/      DimensionScorer, ScoreFusion, DuplicateLevel, AttributionResolver
  llm/        LlmClient, LlmPromptBuilder, LlmResponseParser, LlmCache, CircuitBreaker
  run/        AnalysisRunService, AnalysisRunController, AnalysisExecutor
  result/     AnalysisPairEntity, DuplicateSnapshotEntity, AnalysisQueryService
  support/    ProjectAnalysisInput, AnalysisContext, MatchSnippetExtractor
```

## 2. 数据表（Flyway V4）

```sql
analysis_config(id, version INT, field_weights TEXT, thresholds TEXT,
                top_k INT, simultaneous_days INT, llm_enabled TINYINT,
                local_weight INT, llm_weight INT, llm_top_n INT, llm_timeout_ms INT,
                active TINYINT, created_by, created_at)
analysis_run(id, trigger_type, project_id, config_version, analysis_date DATETIME,
             status, project_count, pair_count, llm_used TINYINT, duration_ms,
             message TEXT, operator_id, created_at)
analysis_pair(id, run_id, project_id, peer_project_id,
              goal_score, content_score, tech_score, milestone_score, tag_score,
              local_score, llm_score NULL, total_score,
              level, relation, direction, earlier_project_id,
              matched_points TEXT, diff_points TEXT,
              llm_reason TEXT NULL, created_at)
duplicate_snapshot(id, run_id, project_id, group_id, analysis_date DATE,
                   max_score, avg_top_score, high_pair_count,
                   cross_group_count, self_group_count, our_earlier_count,
                   peer_earlier_count, simultaneous_count,
                   risk_exposure DECIMAL(14,2), created_at)
llm_call_log(id, run_id, project_id, peer_project_id, model, status,
             latency_ms, prompt_tokens, completion_tokens, error, created_at)
```

- `analysis_config.field_weights` 示例：`{"goal":15,"content":35,"tech":35,"milestone":5,"tag":10}`。
- `thresholds`：`{"high":80,"medium":60,"low":40}`。
- `matched_points`/`diff_points`：`[{"dimension":"tech","terms":["..."],"snippet":"..."}]`。
- 索引：`analysis_run(created_at)`、`analysis_pair(run_id, project_id, total_score)`、`analysis_pair(peer_project_id)`、`duplicate_snapshot(project_id, analysis_date)`、`duplicate_snapshot(group_id, analysis_date)`。
- 删除运行：先删 pair/snapshot，再删 run，单事务。

## 3. 文本处理算法

```text
normalize(text):
  全角->半角；去 HTML 标签；统一标点与空白；英文小写；去控制字符
  截断 20000 字符/维度

tokenize(text):
  tokens = []
  英文/数字: 正则 [a-z0-9_]{2,}
  中文: 连续汉字段 -> 去停用字后生成 2-gram
  术语: TermDictionary 最长匹配（同一技术方向关键词优先）
  术语 token 标记 weight=3，普通 token weight=1
```

- 停用字：`的了和与及或是为在对将把被从到按其这那有为以于而等一个我们项目研究技术系统进行实现基于通过` 等（内置文件），可追加。
- 术语词典：`tech_direction.keywords` + `resources/terms/tech-terms.txt`（内置 200+ 条技术术语）。
- 同一 token 多次出现：`tf = 1 + ln(count)`。
- IDF：在本次运行的全部项目文档集合上统计；单项目分析时以“该项目 + 全部存量项目”为语料。
- 空文本：该维度得分 0，并在报告中标注“文本缺失”。

## 4. 维度与总分

| 维度 | 计算 | 默认权重 |
|------|------|----------|
| goal | 目标文本 TF-IDF 余弦 ×100 | 15 |
| content | 研制内容 TF-IDF 余弦 ×100 | 35 |
| tech | 关键技术（名称+说明）TF-IDF 余弦 ×100 | 35 |
| milestone | 阶段名 Jaccard×0.6 + 时间重叠率×0.4，×100 | 5 |
| tag | 技术方向 + 关键技术名称 Jaccard ×100 | 10 |

```text
localScore = Σ(dimScore_i × weight_i) / Σ(weight_i)
level = localScore >= high ? HIGH : >= medium ? MEDIUM : >= low ? LOW : NONE
```

- 时间重叠率：两项目计划区间交集天数 / 并集天数；任一无日期则为 0。
- 片段提取：每维度取查询项目 Top 5 高权重 token，在原文中定位首次出现，截取前后 60 字，合并为 200 字以内片段；去重。

## 5. 归因

```text
if (sameGroup) relation = SELF_GROUP, direction = SELF_GROUP
else:
  relation = CROSS_GROUP
  earlier = compare(submitTime or createdAt)  // 相等则按 projectId 小者
  deltaDays = abs(days(a.submitTime, b.submitTime))
  if (deltaDays <= simultaneousDays) direction = SIMULTANEOUS
  else direction = earlier == a ? "A_EARLIER" : "B_EARLIER"
  存储 earlier_project_id = earlier.id
```

- 展示层再根据“当前查看项目”解释为 `OUR_EARLIER/PEER_EARLIER`；数据库只存中性事实。
- 趋势/快照中的四类计数按查询项目视角统计。

## 6. 大模型融合

### 6.1 请求

```json
POST {baseUrl}/chat/completions
{
  "model": "configured-model",
  "temperature": 0,
  "response_format": {"type": "json_object"},
  "messages": [
    {"role": "system", "content": "你是科研项目查重专家，只输出 JSON。"},
    {"role": "user", "content": "<项目A结构化摘要>\n<项目B结构化摘要>\n输出字段..."}
  ]
}
```

Prompt 文件：`resources/prompts/duplicate-analysis.txt`，包含输出 JSON schema、评分标准、禁止编造、长度上限。

### 6.2 响应校验

```json
{"semanticScore": 0-100, "overlaps": ["..."], "differences": ["..."],
 "suggestion": "MERGE|EXPLAIN|APPROVE|TERMINATE", "reason": "..."}
```

- 缺字段/越界/类型错误 → 丢弃，记 `llm_call_log.status=INVALID`。
- `overlaps/differences` 每项 ≤200 字，最多 10 项，HTML 转义后存储。
- 解析使用 Jackson `readTree`，禁止 `readValue` 到未知结构。

### 6.3 融合与降级

```text
llmUsed = false
for pair in topN(candidates, llmTopN):
   resp = tryCall(pair)  // 超时 llmTimeoutMs，重试 0
   if resp valid: pair.llmScore = resp.semanticScore; llmUsed = true
   else: consecutiveFailures++; if >=3 break

total = (llmScore != null)
      ? (localW*local + llmW*llm)/(localW+llmW)
      : local
```

- 并发：固定线程池 2；单次运行总大模型耗时上限 `llmTopN × timeout`。
- 熔断：失败计数在单次运行内有效；跨运行不持久化，避免误伤。
- 缓存：`ConcurrentHashMap<String, CacheEntry>`，key=`sha256(projectId|peerId|textHash|configVersion)`，TTL 24h，容量上限 5000。
- 日志：`llm_call_log` 记录状态与 token；不记录 prompt 正文与密钥。

## 7. 运行流程

```text
createRun(trigger, projectId):
  校验并发 -> 读取 active config -> 构建语料（非 DRAFT 项目）
  -> projectId 存在则只对目标项目计算 vs 语料
  -> 计算所有候选 -> 过滤低分/保留 TopK -> 归因 -> 可选 LLM
  -> 批量写 analysis_pair
  -> 聚合 duplicate_snapshot（每项目 1 条）
  -> 更新 project.duplicate_score/risk_level（最新 run）
  -> 更新 run：SUCCESS/FAILED、计数、耗时、llm_used
```

- 事务：pair 与 snapshot 在同一事务；run 状态最后更新；失败时清理本次 run 的 pair/snapshot。
- 幂等：同一项目同一配置重复运行允许，保留历史；趋势按月份取最新。
- 预检：不写 `project.duplicate_score`，只返回结果与 run 记录（`PRECHECK`）。
- 回填：`analysisDate` 非空时（仅 ADMIN/演示），以该日期作为 run/snapshot 的分析日期，语料过滤 `submit_time <= analysisDate`；用于生成历史趋势，不改变项目当前分数（除非是最新一次）。

## 8. 接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/analysis/config` | 登录 | 当前配置 |
| GET | `/api/v1/analysis/config/history` | ADMIN | 历史版本 |
| PUT | `/api/v1/analysis/config` | ADMIN | 保存新版本 |
| POST | `/api/v1/analysis/runs` | PMO/ADMIN | `{triggerType, projectId?}` |
| GET | `/api/v1/analysis/runs` | PMO/ADMIN | 运行历史 |
| GET | `/api/v1/analysis/runs/{id}` | PMO/ADMIN | 运行详情 |
| GET | `/api/v1/analysis/projects/{id}` | 按数据范围 | 项目报告 |
| GET | `/api/v1/analysis/pairs/{id}` | 按数据范围 | 项目对明细 |
| POST | `/api/v1/analysis/precheck` | GROUP_USER/PMO | 临时文本预检 |
| DELETE | `/api/v1/analysis/runs/{id}` | ADMIN | 删除失败运行 |

`GET /analysis/projects/{id}` 返回：

```json
{"project": {...}, "snapshot": {...}, "topPeers": [
  {"pairId":1,"peer":{...},"scores":{"goal":..,"content":..,"tech":..,"milestone":..,"tag":..},
   "localScore":..,"llmScore":..,"totalScore":..,"level":"HIGH",
   "relation":"CROSS_GROUP","direction":"PEER_EARLIER","earlierProjectId":..,
   "matchedPoints":[...],"diffPoints":[...],"llmReason":"..."}
]}
```

## 9. 性能与实现细节

- 预计算每个项目的 token 计数与文本，避免重复分词；300 项目 × 5 字段约 1500 次向量化，内存 < 200MB。
- 相似度只计算 `projectId` 与其他项目；全量运行用上三角遍历，项目对上限 300×299/2≈4.5 万，可行。
- 落库只保留每项目 Top-K + 高分对，典型 < 3000 条。
- 批量插入分批 500 条；H2/MySQL 均使用 `INSERT` 批处理。
- 计算使用 `float`/`double`，落库保留 2 位；比较阈值使用落库后的值，避免精度争议。
- 线程安全：运行上下文不可变；缓存使用并发容器。

## 10. 测试设计

| 测试 | 内容 |
|------|------|
| TokenizerTest | 中英混合、停用词、术语加权、空文本 |
| TfIdfTest | 相同文本相似度 1、无关文本接近 0、IDF 计算 |
| DimensionScorerTest | 节点重叠率、标签 Jaccard、权重归一化 |
| AttributionTest | 同组、他组在先、我方在先、同期边界 30 天 |
| LlmFallbackTest | 关闭、超时、500、非法 JSON、合法响应、缓存 |
| SnapshotTest | 快照计数与 pair 聚合一致、TopK 平均分 |
| RunConcurrencyTest | 并发触发只允许一个运行 |
| PerformanceTest | 300 项目夹具 <5s（CI 可放宽） |

## 11. 风险

- 字符 2-gram 对同义改写不敏感：依赖大模型可选项与术语词典缓解；报告中明确“字面+语义”双口径。
- 大模型响应不稳定：严格校验、熔断、默认关闭；演示不依赖外网。
- 大量项目时 O(n²)：Top-K 截断 + 分批；超过 2000 项目时改为候选召回（标签/关键词倒排）再精算，本期先记录扩展点。
- 权重配置错误：保存时强校验 + 归一化。

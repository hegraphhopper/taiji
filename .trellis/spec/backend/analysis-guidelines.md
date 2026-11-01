# 重复度分析契约（A3 实测）

## 1. Scope / Trigger

修改分析配置、分词/向量/打分/归因、运行编排、快照聚合、分析查询接口时适用。下游趋势（A4）、投入与决策（B）只读本契约产出的结果，不自行实现相似度。

## 2. Signatures

### 分析接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/analysis/config` | 登录 | 当前生效配置 |
| GET | `/api/v1/analysis/config/history` | ADMIN | 全部版本（倒序） |
| PUT | `/api/v1/analysis/config` | ADMIN | 保存新版本（`@Audit`） |
| POST | `/api/v1/analysis/runs` | PMO/ADMIN | `{triggerType, projectId?, analysisDate?}` |
| GET | `/api/v1/analysis/runs` | PMO/ADMIN | 分页 `page/size/triggerType/status/from/to` |
| GET | `/api/v1/analysis/runs/{id}` | PMO/ADMIN | 运行详情 |
| DELETE | `/api/v1/analysis/runs/{id}` | ADMIN | 仅 FAILED，级联删 pair/snapshot |
| GET | `/api/v1/analysis/projects/{id}` | 按数据范围 | `{project,snapshot,topPeers,riskLevel,thresholds,analyzed}` |
| GET | `/api/v1/analysis/pairs/{id}` | 按数据范围 | 双方信息 + 分维度分 + 片段 |
| POST | `/api/v1/analysis/precheck` | GROUP_USER/PMO | `{projectId?}` 或 `{goalText,contentText,techText,milestoneText}` |

### 关键类

```text
AnalysisConfigService.current()/history()/save(AnalysisConfigRequest)
AnalysisRunService.create(AnalysisRunRequest)/list(query)/detail(id)/delete(id)
AnalysisExecutor.execute(trigger, projectId, analysisDate, config, runId) : AnalysisBatch
AnalysisExecutor.precheck(ProjectAnalysisInput, config, runId) : PrecheckOutcome
AnalysisQueryService.report(projectId)/pair(pairId)
ProjectAnalysisInputFactory.build(projectId)/buildAll(List<Project>)
TfIdfVectorizer.vectorize(List<List<WeightedToken>>) : List<SparseVector>
DimensionScorer.score(...) / ScoreFusion.fuse(...) / AttributionResolver.resolve(...)
LlmClient.call(...) / LlmCache.key(...) / LlmCallLogService
```

## 3. Contracts

### 配置

- 单条生效：保存时 `version = max+1`，旧版本 `active=0`，新版本 `active=1`；历史只增不改。
- 校验：五维权重各自 ≥0 且合计 >0；`0 < low < medium < high ≤ 100`；`topK 1..20`；`simultaneousDays 0..365`；本地/大模型权重 ≥0 且合计 >0；`llmTopN 1..20`；`llmTimeoutMs 500..60000`。
- 权重与阈值以 JSON 字符串存 `TEXT`，Jackson 显式序列化；禁止数据库 JSON 类型。
- 新配置只影响之后的运行，历史运行保留原 `config_version`。

### 分词与打分口径

- 归一化：全角转半角、去 HTML/控制符、空白统一、英文小写、截断 20000，null→`""`。
- 分词：中文连续段去停用字后 2-gram；英文/数字按 `[a-z0-9_]{2,}`；术语最长匹配整体成词，权重 3。
- TF-IDF 运行级统一：`tf=1+ln(count)`、`idf=ln((N+1)/(df+1))+1`、L2 归一化。禁止逐对单独建 IDF。
- 维度：goal/content/tech 余弦×100；milestone=阶段名 Jaccard×0.6+时间区间重叠×0.4（任一方无日期则时间项 0）；tag=技术方向+关键技术名 Jaccard×100。
- 总分：`Σ(dim×w)/Σ(该维有文本的 w)`，0–100，落库保留 2 位；分母 0 抛 `ANALYSIS_WEIGHT_INVALID`。
- 等级：`>=high` HIGH、`>=medium` MEDIUM、`>=low` LOW、否则 NONE。比较使用落库后的 2 位值。

### 归因

- 存储只用中性事实：`relation` ∈ `SELF_GROUP/CROSS_GROUP`，`direction` ∈ `SELF_GROUP/SIMULTANEOUS/A_EARLIER/B_EARLIER`，并落 `earlier_project_id`。
- 同组 → SELF_GROUP；跨组 `Δt ≤ simultaneousDays` → SIMULTANEOUS；否则较早方 A_EARLIER/B_EARLIER；时间取 `submit_time`，缺失回退 `created_at`；完全相等按 projectId 小者为先。
- 查询时按「当前项目」翻译为 `OUR_EARLIER/PEER_EARLIER/SIMULTANEOUS/SELF_GROUP`；不得把视角值写库。

### 运行与持久化

- `analysis_pair` 无序对只存一条（`project_id < peer_project_id`），`pair_count` 为唯一对数，趋势不会双计。
- 落库集合 = 任一项目自己 Top-K 的并集；快照 `avg_top_score` 用该项目自己的 Top-K（运行内存计算），不受并集影响。
- 快照每项目每次运行 1 条；`risk_exposure = budget × max_score / 100`（无预算为 null/0）。
- FULL/PROJECT 回写 `project.duplicate_score` 与 `risk_level`；PRECHECK 不回写。
- pair + snapshot 同事务批量写入（500/批）；异常时 run=FAILED、message 非空，按 run_id 清理，不留半成品。
- 并发：同一时刻仅 1 个 FULL/PROJECT，重复触发 409 `ANALYSIS_RUN_CONFLICT`。
- `analysisDate` 仅 ADMIN，语料过滤 `submit_time（或 created_at） <= analysisDate`，snapshot 使用该日期。

### 大模型

- 默认关闭；关闭/未配置时不调用、不写 `llm_call_log`。
- 仅对本地分 Top-N 候选调用；`temperature=0`，要求纯 JSON；`semanticScore 0–100`、数组 ≤10 项且每项 ≤200 字、suggestion ∈ {MERGE,EXPLAIN,APPROVE,TERMINATE}，非法记 INVALID。
- 融合 `(localW×local + llmW×llm)/(localW+llmW)`；失败对保留本地分；单次运行连续失败 3 次熔断。
- `llm_call_log` 只写状态/耗时/token/错误；禁止写 prompt 正文、API Key、项目正文。

- 解释层模式：`localWeight=100 / llmWeight=0` 时总分与趋势不变，只写 `llm_score/llm_reason/llm_overlaps/llm_differences`。
- 连接自检：`POST /api/v1/system/llm/test`（ADMIN），返回状态/耗时/模型，不返回 Key；未配置返回 DISABLED。
- 预检：仅对 Top-1 相似对调用，超时/失败不阻断；`llmUsed=false` 时风险分与规则建议保持本地口径。
- 报告：Word“六、大模型复核”、Excel“模型复核”Sheet；无数据输出“本地算法结果”。

### 查询与权限

- 复用 `ProjectAccessService`：GROUP_USER 本组、EXPERT 已提交（含本组约束）、PMO/ADMIN 全量；项目对明细要求双方至少一方可见。
- 项目报告取该项目最新 `analysis_date` 的快照；`analyzed=false` 表示尚未分析。
- 预检建议动作：HIGH→MERGE、MEDIUM/LOW→EXPLAIN、NONE→APPROVE。

## 4. Validation & Error Matrix

| 条件 | HTTP | 错误码 |
|------|------|--------|
| 配置权重/阈值/范围非法 | 400 | `40040/40041/40042` |
| 预检无任何文本 | 400 | `40043` |
| LLM 配置非法 | 400 | `40044` |
| 运行不存在 | 404 | `40430` |
| 项目对不存在 | 404 | `40431` |
| 快照不存在/未分析 | 404 | `40432` |
| 并发触发运行 | 409 | `40930` |
| 跨组/越权查询 | 403 | `40301` |
| 非 ADMIN 指定 analysisDate | 403 | `40301` |
| 删除 SUCCESS 运行 | 400 | `40000` |

## 5. Good / Base / Bad

- Good：近重复夹具 total=99.80、HIGH、跨组 SIMULTANEOUS；快照 max/high/cross/sim 与 pair 明细一致，敞口=预算×0.998。
- Base：部分重叠夹具 40–60 落 LOW；无关文本 <40 不落库。
- Bad：pair 双向各存一条导致 `pair_count` 翻倍；快照用并集平均导致 `avg_top_score` 与 Top-K 口径不符。
- Bad：把 OUR_EARLIER 写入数据库，换视角查看时归因错乱。
- Bad：LLM 超时抛异常中断整次运行，或把 prompt 正文写进日志。

## 6. Tests Required

- 纯算法（tokenizer/vector/score/attribution/snippet/llm parser/cache）用普通 JUnit，不启动容器。
- 集成（运行/快照/并发/失败清理/预检/analysisDate/权限/删除）用 `@SpringBootTest` + H2 随机库 + MockMvc。
- LLM 用 `LlmHttpTransport` 桩，覆盖关闭、超时、500、非法 JSON、合法融合、连续 3 次熔断、缓存命中。
- 性能：`AnalysisPerformanceTest` 标 `@Tag("perf")`，默认被 surefire `excludedGroups=perf` 排除；手动执行：
  `mvn test "-Dtest=AnalysisPerformanceTest" "-Dtest.excludedGroups=none"`。
- A3 实测：全量 114 项通过（A1/A2 46 + A3 68）；300 项目 FULL 约 1.5s（门禁 5s）。

## 7. Wrong vs Correct

### Wrong：逐对单独建 IDF

```java
for (Pair p : pairs) {
    TfIdfVectorizer v = new TfIdfVectorizer();      // 每对语料只有 2 篇
    double s = v.cosine(a, b);
}
```

### Correct：运行级统一语料

```java
List<SparseVector> vectors = vectorizer.vectorize(allDocs); // 一次构建
double s = CosineSimilarity.of(vectors.get(i), vectors.get(j));
```

### Wrong：跨组方向写入视角值

```java
pair.setDirection("OUR_EARLIER"); // 视角漂移，换人查看即错
```

### Correct：存中性事实，展示层翻译

```java
pair.setDirection(attribution.getDirection()); // A_EARLIER / B_EARLIER / SIMULTANEOUS
// 查询时：earlierProjectId == 当前项目 ? OUR_EARLIER : PEER_EARLIER
```

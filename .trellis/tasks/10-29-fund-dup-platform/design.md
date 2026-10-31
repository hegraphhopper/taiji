# 自投研发项目重复投入治理平台 — 技术设计

## 1. 设计目标与约束

- 在 JDK 8 / Maven 3.5 / Node 18+ 环境可构建，生产 MySQL 8，演示 H2 零依赖。
- 单体后端 + 前后端分离；按 feature 分包，避免过度微服务化。
- 重复度结果必须**可解释、可复算、可回看**：所有得分落库，算法与规则可配置。
- 外部大模型是**可选增强**，任何异常不得阻断主流程；默认关闭。
- 本机无 MySQL/Docker/Python：开发与验收以 H2 演示模式跑通，MySQL/Docker 作为生产交付路径，需在 WeCode 环境验证。

## 2. 技术栈与版本

| 层 | 选型 | 版本 | 说明 |
|----|------|------|------|
| 后端 | Spring Boot | 2.7.18 | JDK 8 兼容的最后一个主线 |
| Web | spring-boot-starter-web / validation / aop | 随 Boot | REST + 参数校验 + 审计切面 |
| 安全 | spring-boot-starter-security + jjwt | 5.7 / 0.11.5 | JWT 无状态鉴权 |
| ORM | MyBatis-Plus | 3.5.3.2 | 分页、逻辑删除、乐观锁 |
| 数据库 | MySQL 8 / H2 | 8.0 / 2.1.214 | H2 使用 `MODE=MySQL` 文件模式 |
| 迁移 | Flyway | 随 Boot（8.5.x） | 同一套 DDL 兼容 MySQL/H2 |
| 文档 | springdoc-openapi-ui | 1.6.15 | `/swagger-ui.html` |
| 文档解析 | Apache POI + PDFBox | 5.2.3 / 2.0.29 | docx、pdf 文本层 |
| 报告导出 | POI（XWPF/SXSSF） | 5.2.3 | 不引入 EasyExcel，避免 POI 版本冲突 |
| 工具 | Lombok | 1.18.30 | 减少样板代码 |
| 前端 | Vue 3 + TypeScript + Vite | 3.4 / 5.4 / 5.4 | Composition API |
| UI | Element Plus | 2.7 | 表单、表格、弹窗 |
| 图表 | ECharts | 5.5 | 趋势、堆叠、热力图、矩形树图 |
| 状态 | Pinia + Vue Router | 2.1 / 4.3 | 登录态与路由守卫 |
| 请求 | Axios | 1.6 | 统一拦截、错误提示 |
| 测试 | JUnit 5 + Mockito + MockMvc + H2 | 随 Boot | 引擎/规则/接口测试 |

大模型调用使用 Spring `RestTemplate` + Jackson，不引入额外 SDK；接口形态为 OpenAI 兼容 `POST {baseUrl}/chat/completions`。

## 3. 总体架构

```text
浏览器（Vue 3 + Element Plus + ECharts）
        │ HTTPS / JSON / JWT
        ▼
Spring Boot 单体（/api/v1）
  ├─ security      认证、JWT、RBAC、数据范围
  ├─ system        用户/角色/项目组/技术方向/审计
  ├─ project       项目、申报内容、状态机、评审、附件
  ├─ analysis      配置、分词、相似度、混合评分、运行、快照
  ├─ insight       趋势归因、投入态势
  ├─ decision      预检、建议规则、决策记录
  ├─ report        Word/Excel/CSV 导出
  └─ integration   LLM 客户端（可降级）
        │ MyBatis-Plus
        ▼
MySQL 8（生产） / H2 文件库（演示）
```

后端目录（`com.taiji.fund`）：

```text
FundApplication.java
common/        Result、PageResult、错误码、异常、枚举、工具、审计注解
config/        Security、MyBatis、Web、Jackson、OpenAPI、上传配置
security/      JwtTokenProvider、JwtFilter、LoginUser、DataScope
system/        user、role、group、techdirection、audit
project/       entity、mapper、service、controller、dto、statemachine
document/      上传、存储、解析、章节抽取
analysis/      config、tokenizer、similarity、llm、run、pair、snapshot
insight/       trend、attribution、investment
decision/      precheck、rule、record
report/        word、excel、csv
integration/   llm client、prompt、cache
```

前端目录（`frontend/src`）：

```text
api/           按模块封装的请求
views/         login、dashboard、project、analysis、trend、investment、decision、system
components/    布局、图表容器、状态标签、空态、表格筛选
stores/        auth、dict、app
router/        路由与守卫
utils/         请求、权限、格式化、下载
styles/        设计 token、全局样式
```

## 4. 数据模型

### 4.1 表清单

| 表 | 用途 | 关键字段 |
|----|------|----------|
| `sys_group` | 项目组/部门树 | id, name, code, parent_id, leader, enabled |
| `sys_user` | 用户 | id, username, password_hash, real_name, group_id, status |
| `sys_role` | 角色 | id, code, name |
| `sys_user_role` | 用户角色 | user_id, role_id |
| `tech_direction` | 技术方向 | id, code, name, parent_id, keywords, enabled |
| `project` | 项目主表 | id, code, name, fund_type, group_id, owner_id, tech_direction_id, budget, status, duplicate_score, risk_level |
| `project_goal` | 项目目标 | id, project_id, content, sort |
| `project_content` | 研制内容 | id, project_id, content, sort |
| `project_key_tech` | 关键技术 | id, project_id, name, description, difficulty, sort |
| `project_milestone` | 节点计划 | id, project_id, name, plan_start, plan_end, deliverable, weight |
| `project_member` | 项目成员 | id, project_id, user_id, role, workload |
| `project_document` | 附件与解析文本 | id, project_id, file_name, file_type, storage_path, text_content, parse_status, parse_message |
| `project_review` | 评审记录 | id, project_id, reviewer_id, round, score, opinion, result |
| `project_approval_log` | 状态流转轨迹 | id, project_id, from_status, to_status, action, operator_id, comment |
| `analysis_config` | 分析配置（版本化） | id, version, field_weights, thresholds, llm_enabled, llm_weight, local_weight, top_k, simultaneous_days, active |
| `analysis_run` | 分析运行 | id, trigger_type, project_id, config_version, status, project_count, pair_count, llm_used, duration_ms, message |
| `analysis_pair` | 项目对结果 | id, run_id, project_id, peer_project_id, goal_score, content_score, tech_score, milestone_score, tag_score, local_score, llm_score, total_score, level, relation, direction, earlier_project_id, matched_points, diff_points |
| `duplicate_snapshot` | 项目级快照 | id, run_id, project_id, group_id, analysis_date, max_score, avg_top_score, high_pair_count, cross_group_count, self_group_count, our_earlier_count, peer_earlier_count, simultaneous_count, risk_exposure |
| `decision_record` | 决策记录 | id, project_id, run_id, risk_score, level, recommendation, reason, similar_projects, savings_estimate, operator_id |
| `llm_call_log` | 大模型调用日志 | id, run_id, project_id, peer_project_id, model, status, latency_ms, prompt_tokens, completion_tokens, error |
| `audit_log` | 审计日志 | id, user_id, action, resource_type, resource_id, detail, ip, result |

### 4.2 通用约定

- 主键 `BIGINT AUTO_INCREMENT`；逻辑删除 `deleted TINYINT DEFAULT 0`；`created_at/updated_at DATETIME`。
- JSON 结构（权重、阈值、重复点、差异点）统一存 `TEXT`，由 Jackson 序列化，保证 MySQL/H2 兼容。
- 金额 `DECIMAL(14,2)`，得分 `DECIMAL(5,2)`，时间统一 `DATETIME`。
- 索引：`project(group_id, status)`、`project(tech_direction_id)`、`project(code)` 唯一、`analysis_pair(run_id, project_id, total_score)`、`duplicate_snapshot(group_id, analysis_date)`、`project_document(project_id)`。
- Flyway 脚本放在 `src/main/resources/db/migration`，仅使用 MySQL/H2 共同支持的语法；`V1__schema.sql`、`V2__seed.sql`。
- H2 URL：`jdbc:h2:file:./data/funddb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`。
- MySQL URL：`jdbc:mysql://host:3306/fund_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`。

## 5. 核心设计

### 5.1 权重与阈值配置

默认配置（`analysis_config` 单条 active 记录，JSON）：

```json
{
  "fieldWeights": {"goal": 15, "content": 35, "tech": 35, "milestone": 5, "tag": 10},
  "thresholds": {"high": 80, "medium": 60, "low": 40},
  "topK": 5,
  "simultaneousDays": 30,
  "llm": {"enabled": false, "localWeight": 70, "llmWeight": 30, "topN": 5, "timeoutMs": 8000}
}
```

- 字段权重在运行时归一化，防止配置错误导致分数越界。
- 阈值比较：`score ≥ high` 为高，`≥ medium` 为中，`≥ low` 为低，否则无明显重复。
- 配置变更写审计日志；新配置仅影响之后发起的运行，历史快照不重算。

### 5.2 文本处理与相似度

1. 归一化：全角转半角、去除 HTML/控制字符、统一标点、英文小写。
2. 分词：
   - 中文：字符 2-gram（去停用字/数字噪声）作为基础特征；
   - 术语增强：从 `tech_direction.keywords` 与内置术语表匹配，命中词加权 ×3；
   - 英文/编号：按单词切分并保留。
3. 向量化：对语料（本次运行涉及的全部项目）计算 TF-IDF；查询项目与存量项目共用同一 IDF。
4. 分维度余弦相似度，映射到 0–100。
5. 标签相似度：技术方向 + 关键技术名称集合 Jaccard。
6. 节点相似度：阶段名称集合 Jaccard × 0.6 + 计划起止时间重叠率 × 0.4。
7. 本地总分：按字段权重加权求和。
8. 匹配片段：对每个维度取 TF-IDF 权重最高的前 5 个词，结合原文句子定位，生成“重复点”高亮文本（截断 200 字）。

确定性要求：相同输入 + 相同配置 → 相同结果；不依赖随机数。

### 5.3 混合大模型评分

```text
if (!llm.enabled || !configured) return local;
candidates = topN by localScore (仅高/中等级候选)
for pair in candidates (并发 ≤ 2):
    prompt = 结构化字段 + 双方项目摘要
    resp = POST {baseUrl}/chat/completions   // temperature=0, JSON 输出
    validate JSON：{semanticScore:0-100, overlaps:[], differences:[], suggestion}
    on timeout/error/invalid → 记 llm_call_log，跳过该对
final = weighted(localScore, llmScore)   // 仅成功对；失败对用 localScore
```

- Prompt 模板：`src/main/resources/prompts/duplicate-analysis.txt`，要求只输出 JSON、不得编造、不得返回 Markdown。
- 安全：发送内容仅限结构化文本（截断），不发送附件二进制、用户手机号等；API Key 只从环境变量/加密配置读取，响应日志脱敏。
- 熔断：单次运行连续 3 次失败即停止后续调用，本次运行标记 `llm_used=false`，不重试整轮。
- 缓存：按 `sha256(projectId|peerId|字段文本|configVersion)` 缓存 24h（内存 Caffeine 或简单 ConcurrentHashMap + TTL），避免重复计费。
- 成本可视化：`llm_call_log` 记录 token 与耗时，管理端可查。

### 5.4 归因与快照

对每个相似对（总分 ≥ low 阈值）：

```text
relation = sameGroup ? SELF_GROUP : CROSS_GROUP
if sameGroup: direction = SELF_GROUP
else:
  days = abs(submitTimeA - submitTimeB) in days
  if days <= simultaneousDays: direction = SIMULTANEOUS
  else if A.submitTime < B.submitTime: direction = (A.group == viewerGroup ? OUR_EARLIER : PEER_EARLIER)
  else: direction = (B.group == viewerGroup ? OUR_EARLIER : PEER_EARLIER)
```

- 存储时保存**中性** `earlierProjectId`，展示时按当前视角解释为我方/他方，避免视角耦合。
- 快照按项目聚合：`max_score`、Top-K 平均分、高风险对数、四类归因计数、风险敞口。
- 趋势查询：按 `analysis_date` 月份分组，同项目同月取最新 run；跨月保留历史，天然形成趋势。
- 归因一致性：趋势计数直接由 `analysis_pair` 聚合，快照冗余字段用于性能，单元测试校验两者一致。

### 5.5 投入态势与风险敞口

- 统计口径只含 `APPROVED / IN_PROGRESS / COMPLETED`（可配置是否含结项）。
- 指标计算在服务层完成，SQL 只做过滤与分组，避免数据库方言差异。
- 风险敞口 = Σ(预算 × max_score/100)；同时给出“高风险敞口”（仅 high 级）。
- 同比 = 当前年度 / 上一年度 - 1；无上一年数据时返回 `null` 并前端显示“—”。

### 5.6 辅助决策规则

```text
if score >= 80 && (relation == SELF_GROUP || direction == PEER_EARLIER): MERGE 或 TERMINATE
else if score >= 60: EXPLAIN
else: APPROVE
```

- 规则表存配置，默认按上述；大模型仅补充理由，不覆盖规则动作。
- 预计重复投入 = 预算 × score / 100，四舍五入到元。
- 建议专家：同技术方向、启用状态、按进行中评审数升序取前 3；无匹配时回退到基金办。

### 5.7 状态机

```text
DRAFT --submit--> SUBMITTED --accept--> REVIEWING --approve--> APPROVED
  ▲                   │                     │
  └----withdraw-------┘                     └--reject--> REJECTED --resubmit--> SUBMITTED
APPROVED --start--> IN_PROGRESS --complete--> COMPLETED
IN_PROGRESS --terminate--> TERMINATED
```

- 转移表集中在 `ProjectStateMachine`，非法转移抛业务异常（400 + 错误码）。
- 每次转移写 `project_approval_log` + 审计日志；只有 `DRAFT/REJECTED` 可编辑。

### 5.8 附件解析

- 白名单：`.docx`、`.pdf`、`.txt`、`.md`；单文件 ≤ 20MB；存储到 `storage/{yyyyMM}/{uuid}.{ext}`，原始文件名入库不做路径拼接。
- docx：POI `XWPFWordExtractor`；pdf：PDFBox `PDFTextStripper`；编码：UTF-8，异常字符替换。
- 章节抽取：按标题关键词（项目目标/研究目标/研制内容/研究内容/关键技术/节点计划/进度安排/考核指标）与编号模式切分；命中则生成结构化草稿，未命中则整体作为研制内容候选并提示人工确认。
- 解析在请求线程内完成（20MB 内可控）；失败记录 `parse_status=FAILED` 与原因，不影响项目保存。
- 安全：校验扩展名 + 文件头；禁止 SVG/可执行文件；下载使用服务端 ID，不接受客户端路径。

## 6. 接口设计

统一前缀 `/api/v1`，统一响应：

```json
{"code": 0, "message": "ok", "data": {}, "traceId": "..."}
```

分页：`{"records": [], "total": 0, "page": 1, "size": 10}`。错误码：`400xx` 参数/状态错误，`401xx` 认证，`403xx` 权限，`404xx` 不存在，`500xx` 系统错误。

| 模块 | 主要接口 |
|------|----------|
| 认证 | `POST /auth/login`、`POST /auth/logout`、`GET /auth/me`、`PUT /auth/password` |
| 系统 | `GET/POST/PUT/DELETE /groups`、`/users`、`/roles`、`/tech-directions`、`GET /audit-logs` |
| 项目 | `GET/POST/PUT/DELETE /projects`、`GET /projects/{id}`、`POST /projects/{id}/submit|withdraw|review|approve|reject|start|complete`、`GET /projects/{id}/logs` |
| 申报内容 | 项目详情聚合读写；`PUT /projects/{id}/sections` 批量保存目标/内容/技术/节点/成员 |
| 附件 | `POST /projects/{id}/documents`、`GET /documents/{id}/download`、`POST /documents/{id}/parse`、`DELETE /documents/{id}` |
| 分析 | `GET/PUT /analysis/config`、`POST /analysis/runs`、`GET /analysis/runs`、`GET /analysis/runs/{id}`、`GET /analysis/projects/{id}`、`GET /analysis/pairs/{id}`、`POST /analysis/precheck` |
| 趋势 | `GET /insight/trend`、`GET /insight/attribution`、`GET /insight/heatmap`、`GET /insight/trend/export` |
| 投入 | `GET /insight/investment/overview`、`/by-dimension`、`/risk-exposure`、`GET /insight/investment/export` |
| 决策 | `POST /decision/precheck`、`POST /decision/records`、`GET /decision/records`、`GET /decision/records/{id}/report` |
| 报告 | `GET /reports/projects/{id}/duplicate.docx`、`GET /reports/decisions/{id}.docx`、`GET /reports/decisions/{id}.xlsx` |

鉴权：`Authorization: Bearer <token>`；前端 401 统一跳登录，403 显示无权限页。

## 7. 安全设计

- 密码 BCrypt（strength 10）；登录失败计数存内存/表，超限锁定 10 分钟。
- JWT：HS256，密钥 ≥ 32 字节，从 `JWT_SECRET` 读取；有效期 8h；payload 只含 userId/username/roles，不含敏感信息。
- 数据范围：`DataScopeInterceptor`/服务层根据角色注入 `group_id` 条件；所有按 ID 读取项目后再次校验归属。
- 上传：扩展名 + 文件头双校验；存储目录不在 Web 根目录；下载 `Content-Disposition: attachment`。
- 输出：统一 Jackson 配置；富文本仅允许纯文本，前端 `v-text`/插值，禁止 `v-html` 渲染用户内容。
- SQL：全部参数绑定；排序字段白名单。
- 审计：AOP 注解 `@Audit(action, resourceType)` 记录操作人、IP、资源、结果；导出与配置变更必须记录。
- 密钥管理：`.env.example` 提供占位；仓库不提交真实密钥；`application.yml` 不写死密码。

## 8. 部署与运行

### 8.1 演示模式（默认，评审优先）

```powershell
mvn -q -DskipTests package
java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo
```

- H2 文件库 `./data/funddb`，Flyway 自动建表与种子数据。
- 前端开发：`cd frontend; npm install; npm run dev`（代理 `/api` 到 8080）。
- 单包演示：`npm run build` 后将 `dist/*` 复制到 `src/main/resources/static`，后端同时提供页面与 API。

### 8.2 生产模式

- `SPRING_PROFILES_ACTIVE=prod`，环境变量：`DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET/LLM_BASE_URL/LLM_API_KEY/LLM_MODEL`。
- 提供 `docker-compose.yml`（mysql:8.0 + app）与 `Dockerfile`（maven 构建 + JRE 8 运行）。
- WeCode：以 Docker 或“jar + 静态资源”方式部署，暴露 8080；健康检查 `/api/v1/health`。
- 数据库备份：`scripts/backup-db.ps1`（mysqldump）与 H2 文件拷贝说明；`db/schema.sql` + `db/data.sql` 作为可恢复基线。

## 9. 关键取舍与风险

| 决策 | 取舍 | 缓解 |
|------|------|------|
| 单体而非微服务 | 牺牲独立扩展，换取部署简单与赛题交付确定性 | 按 feature 分包，后续可拆 |
| 字符 n-gram + TF-IDF 而非向量模型 | 语义能力弱于 embedding，但零依赖、可解释、离线可用 | 外部大模型语义复核补足 |
| 大模型默认关闭 | 演示不依赖外网与密钥，但默认效果偏字面 | 提供开关、配置样例与降级日志；评审环境可现场开启 |
| JSON 存 TEXT | 无法用数据库 JSON 函数查询 | 查询在应用层完成，快照表承担聚合 |
| 固定状态机 | 不支持任意流程编排 | 覆盖赛题与管理办法，动作权限可配 |
| H2 兼容 DDL | 部分 MySQL 特性不可用 | 避免 JSON/全文索引/存储过程，统一标准 SQL |
| 无 OCR | 扫描件无法解析 | 明确上传提示与失败原因，支持人工录入 |

## 10. 兼容性与数据迁移

- Flyway 版本只增不改；演示库升级自动执行。
- 字段权重/阈值变更不重算历史快照，趋势保持稳定；需要重算时发起新 run。
- 旧 run 结果保留，报告可复现当时口径（快照存 config 版本号）。

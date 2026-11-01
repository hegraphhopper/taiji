# 系统设计

## 1. 总体架构

```text
Vue 3 + Vite + Element Plus + ECharts
            │ HTTP / JWT
Spring Boot 2.7 + Spring Security + MyBatis-Plus
            │
   H2（demo）/ MySQL 8（prod） + Flyway
```

- 后端分层：`controller → service → mapper → domain/dto`。
- 前端按 feature：`views/project|analysis|trend|investment|decision|review|system|tasks`。
- 统一响应 `{code,message,data,traceId}`；错误码与 HTTP 状态一致。

## 2. 模块

| 模块 | 职责 |
|------|------|
| security | 登录、JWT、角色、数据范围、审计切面 |
| project | 项目、申报书分段、附件解析、状态机、评审、审批轨迹 |
| analysis | 配置、运行、相似度、快照、报告查询、LLM 解释 |
| insight | 趋势、归因、热力图、投入态势 |
| decision | 预检、规则、专家建议、决策记录、Word/Excel |
| system | 用户、项目组、技术方向、角色、系统参数、审计 |
| task | 四角色待办 |
| bootstrap | demo 账号与 60 项目演示数据 |

## 3. 关键算法

- 文本向量：中文切分 + 字段权重 + TF-IDF；字段权重默认 `goal 15 / content 35 / tech 35 / milestone 5 / tag 10`。
- 等级阈值默认 `high 80 / medium 60 / low 40`。
- 相似对：Top-K 并集，保留 `matched_points` 与 `diff_points` 可解释片段。
- 归因：按提交时间与项目组判定 `SELF_GROUP / PEER_EARLIER / OUR_EARLIER / SIMULTANEOUS`。
- 大模型：仅对 Top-N 候选调用；JSON 严格校验、缓存、熔断、超时降级；`llm_overlaps/llm_differences` 落库。
- 解释层模式：`localWeight=100 / llmWeight=0` 时总分不变，只提供模型理由。

## 4. 数据流

```text
申报书 → ProjectAnalysisInput → 维度分 → 本地总分
  → Top-K 并集 → （可选）LLM 复核 → analysis_pair
  → duplicate_snapshot（按 analysisDate）→ 趋势/归因/投入/预检
```

- 历史回填按月末生成快照，不回写项目当前分数。
- 立项 PASS 门禁：项目全部轮次中不同专家 PASS 数 ≥ `review.requiredPassCount`（默认 2）。

## 5. 权限与审计

- `PermissionCatalog` 与前端 `utils/permission.ts` 同源。
- 项目动作：ADMIN 全部；GROUP_USER 同组创建/编辑/提交/撤回/开始/结项；PMO 审查/立项/驳回/终止；EXPERT 评审。
- 所有写接口 `@Audit`；审计详情脱敏，最长 1900 字符；仅 ADMIN 查询/导出。

## 6. 部署

- 本地：`scripts/start-demo.ps1`（H2 + Vite）。
- 单机：`java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo`。
- Docker：`Dockerfile` 多阶段构建前端并打入 jar；`docker-compose.yml` 提供 MySQL 8 + 应用。
- 生产：`prod` profile + 环境变量；`JWT_SECRET`、`ADMIN_INIT_PASSWORD` 必填。

## 7. 关键取舍

- H2 文件库保障评审零依赖；MySQL 兼容语法由 Flyway 统一维护。
- LLM 默认关闭，失败降级；解释层模式避免趋势口径漂移。
- 审批状态机固定，动作权限集中校验，避免自定义流程复杂度。

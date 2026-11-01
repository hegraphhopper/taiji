# 交付报告（进行中）

## D0 演示数据基线（2026-10-31 完成）

### 成果

- `DemoDataInitializer` 替换旧 8 项目 seeder：60 个项目、2024–2026 提交时间、8 个项目组、7 种状态、预算 30–268 万。
- 近似主题 10 对：同组 2 对、跨组间隔 3 个月 6 对、跨组 10 天内 2 对；其余 40 个独立主题。
- 每个项目含 1 目标、2 研制内容、3 关键技术、3 节点、2 成员。
- 开关 `DEMO_DATA_SEED`（默认 true），`test` profile 跳过，`DEMO-` 前缀幂等。

### 验证

| 项 | 结果 |
|----|------|
| 项目总数 | 60 |
| 状态覆盖 | DRAFT 4 / SUBMITTED 8 / REVIEWING 4 / APPROVED 18 / REJECTED 4 / IN_PROGRESS 18 / COMPLETED 4 |
| 项目组 | 8 个组均有项目（7–8 个/组） |
| 趋势点 | 14（2025-08 ~ 2026-09） |
| 四类归因 | self 32 / our 143 / peer 143 / simultaneous 18 |
| 归因合计 | 336 = Σ pairCount |
| 草稿排除 | 分析项目数 56 = 60 − 4 DRAFT |
| 重启幂等 | 项目数仍为 60 |
| 回归测试 | `mvn -q test` 136/136 通过 |

### 遗留（D 阶段）

- 20+ 项目附件元数据与 4–6 份样例文档（docx/pdf/md）
- MySQL 脚本、备份恢复、文档、截图、Docker/WeCode 部署

## D1–D6 交付执行记录（2026-11-01）

### 已完成
- 文档：README 更新；新增 requirements-analysis、design、database、api、user-guide、metrics、demo-data、demo-script、submission-checklist。
- 截图：docs/screenshots/core 8 张（工作台、项目列表、项目详情、分析运行、趋势、投入、决策、决策记录）；治理 16 张，共 24 张。
- 数据库：H2 备份 db/backup/fund-platform-20261101-100939.zip（0.22 MB），解压校验 funddb.mv.db 1,634,304 字节一致；新增 backup-db.ps1 / restore-db.ps1。
- MySQL：db/mysql/schema.sql（Flyway V1–V7）、data.sql、README。
- 部署：Dockerfile（Node+Maven 多阶段，前端打入 jar）、docker-compose.yml、.dockerignore。
- 大模型扩展：连接自检、解释层、预检 Top-1、V7 落库、Word/Excel 模型复核段；全量测试 174/174。

### 未完成/阻塞
- WeCode 部署地址：未提供入口，未执行。
- Docker Compose 运行验证：本机无 Docker，仅静态配置。
- MySQL 8 导入：本机无实例，未实测。
- 真实大模型联调：未注入 LLM_BASE_URL/LLM_API_KEY/LLM_MODEL。
- 附件样例与 20+ 附件元数据：未生成。
- 远程推送：GitHub 仓库 hegraphhopper/taiji 对 keywsky 无写权限；本地提交完成，push 403。

### 验证
- mvn -q test：174/174。
- mvn -q -DskipTests package：退出码 0。
- npm run typecheck / build：退出码 0。
- 自检未配置：DISABLED；预检降级：llmUsed=false，Top-1 35.03。

### 真实大模型联调（2026-11-01）

- 配置：项目根目录 `application-local.yml`（已 gitignore，未提交）；`base-url=https://api.deepseek.com`，`model=deepseek-flash`。
- 启动：`SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:./application-local.yml`；JDK 8 证书问题通过 `-Djavax.net.ssl.trustStoreType=Windows-ROOT` 解决（已写入启动脚本）。
- 连接自检：`success=true`，985 ms，模型 `deepseek-flash`。
- FULL 分析运行 #24：`llmUsed=true`，56 个项目、19 个相似对、20.8 s。
- 项目 #2：最高重复度 91.60；Top-1 `totalScore=91.60`、`localScore=91.60`、`llmScore=90`，包含模型理由/重复点/差异点。
- 预检项目 #42：`llmUsed=true`，风险分 35.03 与本地一致，模型分 76，建议动作不变。
- 模型调用日志：8 次调用，7 成功、1 失败，平均 3393 ms。
- Word 报告含“六、大模型复核”、模型重复点/差异点；Excel 含“模型复核”Sheet。

### 大模型收口（2026-11-01）

- 默认权重改为解释层 100/0：`V4` 种子、`AnalysisConfigService` 缺省值、前端表单同步；新增 `V8__llm_explanation_default.sql` 迁移旧库 70/30（仅 `llm_enabled=0`）。
- `LlmClient` 增加有界重试：超时/连接异常/5xx 最多 2 次尝试；4xx 与非法 JSON 不重试；熔断按调用计数，日志每次调用一条。
- 可复现：`.env.example` 增加 `LLM_*`；`start-demo.ps1` 检测 `application-local.yml` 自动加载；README 与用户手册写明口径与限制。
- 验证：`mvn -q test` 178/178；`npm run typecheck`、`npm run build` 退出码 0。
- 仍存限制：串行调用、进程内 24h 缓存、单一 OpenAI 兼容端点。

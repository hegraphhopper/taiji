# 交付物与部署验证 — 实施计划

## 1. 顺序清单

- [ ] 冻结功能：所有子任务验收完成，记录版本与 commit。
- [ ] `DemoDataInitializer`：60 项目、子表、成员、附件元数据、重复模式、幂等。
- [ ] `DemoDocxGenerator`：由开源文档摘要 + 项目文本生成 2 份 docx 样例。
- [ ] `scripts/fetch-samples.ps1`：下载开源文档、校验、失败回退、写来源清单。
- [ ] `DemoBackfillRunner`：14 个月历史分析与快照；重复执行跳过。
- [ ] 启动 demo，人工核对趋势、归因、投入、预检数字。
- [ ] `db/mysql/schema.sql`、`data.sql`；与 Flyway 对齐。
- [ ] `scripts/backup-db.ps1` / `restore-db.ps1`；生成实际备份文件并演练恢复。
- [ ] `Dockerfile`、`docker-compose.yml`、`nginx.conf`、`.env.example`。
- [ ] README（环境、启动、账号、功能、部署）。
- [ ] docs 全套（需求、设计、数据库、接口、用户手册、指标、演示数据、演示脚本、提交清单）。
- [ ] Playwright 截图 ≥10 张，检查控制台与敏感信息。
- [ ] 权限矩阵、端到端回归、构建与测试全量执行。
- [ ] 仓库清理与 `.gitignore` 检查；记录 WeCode 部署结果。
- [ ] 更新提交清单并请用户确认后提交（提交需用户明确指示）。

## 2. 验证命令

```powershell
# 全量构建
mvn -q clean test
mvn -q -DskipTests package

# 前端
cd frontend; npm ci; npm run build; cd ..

# 启动演示（首次自动建库 + 演示数据 + 回填）
java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

# 健康与关键接口
Invoke-RestMethod http://localhost:8080/api/v1/health
Invoke-RestMethod "http://localhost:8080/api/v1/insight/trend?startMonth=2025-08&endMonth=2026-10" -Headers $h
Invoke-RestMethod "http://localhost:8080/api/v1/insight/investment/overview" -Headers $h

# 备份 / 恢复
pwsh scripts/backup-db.ps1 -Profile demo
pwsh scripts/restore-db.ps1 -Archive db/backup/fund-platform-20261029.zip

# 截图（Playwright MCP 或脚本）
# 登录 http://localhost:8080 -> 逐页截图到 docs/screenshots/
```

- 恢复验证：移动 `data/` 后解压备份，启动并检查项目数、趋势点数、登录。
- 仓库扫描：`git status --short`、`git diff --cached`，确认无 `.env`、`data/`、`storage/`、`node_modules/`、密钥。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| `DemoDataInitializer` | 重复插入/污染正式库 | 仅 demo profile；幂等检查；生产禁用 |
| `DemoBackfillRunner` | 覆盖最新分数 | 回填后重跑当前分析；提供配置关闭 |
| `schema.sql` | 与 Flyway 漂移 | 由 Flyway 生成基线后校对；交付前 diff |
| 备份/恢复 | 文件锁导致损坏 | 停服备份、CHECKPOINT、校验压缩包 |
| 截图 | 泄露 Token/路径 | 仅演示数据、截图前清理、逐张检查 |
| Docker | 本机无法验证 | 明确标注未验证；wecode 环境补验 |

## 4. 完成定义（DoD）

- 全新环境按 README 一条命令启动，5 分钟内看到趋势、归因、投入、决策数据。
- 演示数据覆盖四类归因与三档风险；趋势 ≥12 个月。
- 备份文件实际恢复成功并记录命令与结果。
- 文档与页面一致，截图齐全，演示脚本可照读。
- 全量构建/测试/前端构建通过；权限矩阵与端到端回归通过。
- 仓库无密钥与敏感数据；提交清单完成。
- WeCode 部署成功或如实记录阻塞与替代验证方式。

## 5. 提交前检查（用户确认后执行）

- [ ] `git status` 仅包含预期文件；无临时日志、无 `target/`、无 `node_modules/`。
- [ ] README 中的地址/端口/账号与实际一致。
- [ ] 数据库备份路径与门户填报一致。
- [ ] 演示视频/截图文件名与文档引用一致。
- [ ] 获得用户明确提交指示后再 `git add/commit`。

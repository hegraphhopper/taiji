# 开发报告 — A1 平台底座与最小鉴权

## 成果摘要

A1 已完成：项目从 HelloWorld Maven 骨架升级为可运行的 Spring Boot 2.7 + Vue 3 平台底座。后端具备 Flyway 建库与种子、JWT 登录、账号锁定、角色与数据范围基础设施、审计写入、组织/技术方向/字典只读接口；前端具备登录、主布局、路由守卫、工作台占位、401/403/404 与字典缓存。A1 的 22 条单元测试全部通过，前后端可一条脚本启动。

## 已完成需求

- 工程骨架：坐标 `com.taiji:fund-platform:1.0.0`，Java 8，Spring Boot 2.7.18，MyBatis-Plus 3.5.3.2，Flyway，H2/MySQL 双 profile。
- 数据层：`V1__schema.sql` 建 6 张系统表；`V2__seed_core.sql` 写入 4 角色、8 项目组、12 技术方向、4 演示账号；逻辑删除、时间戳与索引。
- 认证：登录/登出/当前用户/改密；JWT 8 小时；失败 5 次锁定 10 分钟；成功清零；锁定期结束重新计数。
- 安全：prod 强制 `JWT_SECRET`；禁用用户 token 立即失效；401/403 统一 JSON；框架异常 404/405/415 不再落 500。
- 权限基础设施：`RoleCode`、`SecurityUtils`、`DataScopeService`、`@PreAuthorize` 骨架；`ADMIN/PMO` 全量，`GROUP_USER` 本组，`EXPERT` 待 governance 明确。
- 审计：`@Audit` + AOP 覆盖登录/登出/改密；密码/Token 脱敏；异常记 FAIL；写审计失败不阻断主流程。
- 只读接口：`/dicts`、`/groups`、`/tech-directions`。
- 前端：Vue 3 + TS + Vite 5 + Element Plus + Pinia + Router + Axios；登录页、主布局、守卫、工作台、403/404、字典缓存、改密弹窗、登出。
- 运行脚本：`scripts/start-demo.ps1`（隐藏窗口、健康检查）、`stop-demo.ps1`（PID 树清理与归属校验）、`status-demo.ps1`。
- 文档：README、A 阶段产品设计书、后端/前端规范更新。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn -q clean package` | 退出码 0，产出 `target/fund-platform-1.0.0.jar`（62.8 MB） |
| 单元测试 | 22/22 通过，100%；见 `test/unit/report-round-1.md`、`report-round-2.md` |
| `npm.cmd run typecheck` | 退出码 0 |
| `npm.cmd run build` | 退出码 0，1689 模块，产物写入 `frontend/dist` |
| 启动脚本 | `scripts/start-demo.ps1 -SkipBuild` 退出码 0，后端 PID 23368、前端 node PID 4216 |
| 健康检查 | `/api/v1/health` HTTP 200，profile=demo，db=UP |
| 登录 | `admin / Admin@123` 成功，角色 ADMIN；`/auth/me` 无密码哈希等敏感字段 |
| 浏览器验收 | 登录、刷新保持、无效 token 清理、已登录访问 /login、403、404、登出、900/1440 布局均通过；正常流程控制台 0 error |
| 审计 | SQL 日志与测试确认 LOGIN/LOGOUT 记录，密码字段为 `***` |
| 检查报告 | `check.md` 结果 passed |

## 关键实现说明

- 登录失败计数使用 `LoginAttemptService.REQUIRES_NEW` 独立事务，避免业务异常回滚导致锁定失效。
- `JwtAuthenticationFilter` 每次请求重新加载用户状态，禁用用户旧 token 立即失效。
- `GlobalExceptionHandler` 显式处理 405/415/404，保留 `Exception` 兜底；响应统一 `Result`。
- 前端 401 清除 token 并带 redirect 跳登录；403 跳 `/403`；字典失败不误踢登录态。
- 启动脚本不使用 `-NoNewWindow`，避免子进程继承输出管道导致工具调用长时间挂起。

## 遗留问题与风险

- Flyway 8.5.13 对 H2 2.1.214 提示“未测试版本”，迁移与测试均成功；后续可固定 H2 2.1.210 或升级 Flyway。
- `EXPERT` 数据范围当前受限（非 ADMIN/PMO 放行），与 design 中“按业务规则”表述需在 governance 接入评审指派后确认。
- 403 端到端使用测试专用 Controller 验证；生产接口暂无仅管理员接口，完整权限矩阵在 governance 补齐。
- JWT 存 `localStorage`，存在 XSS 风险；后续可评估 HttpOnly Cookie。
- 登录页演示账号明文提示不区分 demo/prod 构建；交付前可加开关。
- MySQL 8 与 Docker 未在本机验证，需在 WeCode/目标环境补验；演示以 H2 为准。
- `.mvn` 未使用；构建依赖本机 Maven 3.5.0。

## 运行方式

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-demo.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/status-demo.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-demo.ps1
```

- 前端：http://127.0.0.1:5173
- 后端健康：http://localhost:8080/api/v1/health
- Swagger：http://localhost:8080/swagger-ui.html
- 账号：`admin / Admin@123`、`pmo01 / Pmo@12345`、`user01 / User@12345`、`expert01 / Expert@12345`

## 相关文档

- `README.md`
- `docs/A阶段产品设计书.md`
- `.trellis/tasks/10-29-fund-foundation/prd.md`、`design.md`、`implement.md`
- `.trellis/tasks/10-29-fund-foundation/check.md`
- `.trellis/tasks/10-29-fund-foundation/test/用例设计.md`
- `.trellis/tasks/10-29-fund-foundation/test/unit/report-round-1.md`、`report-round-2.md`

# 平台底座与最小鉴权（核心先行）— 实施计划

## 1. 顺序清单

- [x] 备份/移除 HelloWorld 骨架；改写 `pom.xml`（坐标、依赖、插件、Java 8）。
- [x] 建 `application.yml`、`application-demo.yml`、`application-prod.yml`、`.env.example`、`.gitignore`。
- [x] 建 `common`（Result/PageResult/ErrorCode/BizException/GlobalExceptionHandler/枚举/工具）。
- [x] 建 Flyway `V1__schema.sql`、`V2__seed_core.sql`，本地 H2 启动验证。
- [x] 建 MyBatis-Plus 配置、分页、逻辑删除、审计字段自动填充。
- [x] 建 `security`（JWT、过滤器、UserDetails、登录服务、锁定策略）与 `SecurityConfig`。
- [x] 建 `system` 首版：登录/当前用户、组织与技术方向只读接口、字典聚合；完整 CRUD 留到 governance。
- [x] 建 `@Audit` 注解与 AOP 切面；接入登录、密码变更（业务写操作后续接入）。
- [x] 建 OpenAPI 分组、健康检查。
- [x] 后端单测（认证、鉴权基础设施、数据范围钩子、审计写入、Flyway 冒烟）。
- [x] 前端脚手架：Vite/Vue/TS/Element Plus/Pinia/Router/Axios。
- [x] 登录页、主布局、路由守卫、401/403/404、字典缓存。
- [x] 首页占位；系统管理页面移交 governance。
- [x] README 初版与演示账号说明。
- [x] 全量验证并记录结果。

## 2. 验证命令

```powershell
mvn -q clean package
mvn test
java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

# 健康检查与登录
Invoke-RestMethod http://localhost:8080/api/v1/health
$body = @{username='admin';password='Admin@123'} | ConvertTo-Json
$resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType 'application/json' -Body $body
$token = $resp.data.token
Invoke-RestMethod http://localhost:8080/api/v1/auth/me -Headers @{Authorization="Bearer $token"}
Invoke-RestMethod http://localhost:8080/api/v1/dicts -Headers @{Authorization="Bearer $token"}
Invoke-RestMethod http://localhost:8080/api/v1/tech-directions -Headers @{Authorization="Bearer $token"}

cd frontend
npm install
npm run build
npm run dev
```

- 负向验证：无 token 访问 `/api/v1/dicts` 返回 401；用 `user01` 的 token 访问仅管理员接口返回 403。
- 审计验证：登录后直接查 `audit_log` 表，确认有 LOGIN 记录且无明文密码（查询接口在 governance 实现）。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| `pom.xml` | 依赖冲突导致无法构建 | 保留原 pom 备份；失败时回到 HelloWorld 版本再逐个加依赖 |
| `V1__schema.sql` | H2/MySQL 语法差异 | 拆分方言脚本；演示库删 `data/` 重建 |
| `SecurityConfig` | 误放行接口 | 单测覆盖匿名/角色矩阵；按路径复核 |
| `application-prod.yml` | 密钥缺失导致启动失败 | 属预期；提供 `.env.example` 与 README 说明 |
| 种子密码 | 明文进入仓库 | 只存 BCrypt 哈希；文档写演示密码 |

## 4. 完成定义（DoD）

- 所有验收项勾选并附实际命令输出摘要。
- `mvn test` 通过；`npm run build` 通过。
- H2 演示启动 1 次冷启动成功，重启后数据保留。
- 审计日志、权限矩阵、锁定策略有人工验证记录（写入本文件或 `report.md`）。
- 父任务设计中的数据范围基础设施可供 declaration 直接复用。

## 5. 交接给下游

- 统一响应、错误码、分页、审计注解、当前用户工具、数据范围工具的用法写入 `README` 或代码注释外的 `docs/dev-guide.md`。
- 字典接口字段稳定后再开发项目表单。

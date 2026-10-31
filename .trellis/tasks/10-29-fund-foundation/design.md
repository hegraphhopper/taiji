# 平台底座与最小鉴权（核心先行）— 技术设计

> 首版实现范围为登录/当前用户/字典只读/组织与技术方向只读/审计写入；下表与设计中的完整管理能力保留为 governance 阶段目标，避免后期重构表结构与安全模型。

## 1. 工程与依赖

```text
pom.xml                         # 单模块，jar packaging
src/main/java/com/taiji/fund/
  FundApplication.java
  common/       api(Result, PageResult, ErrorCode), exception, enums, util, audit
  config/       SecurityConfig, MybatisConfig, WebConfig, JacksonConfig, OpenApiConfig
  security/     JwtProperties, JwtTokenProvider, JwtAuthenticationFilter, LoginUser, UserDetailsServiceImpl
  system/       user, role, group, techdirection, dict, audit
src/main/resources/
  application.yml
  application-demo.yml
  application-prod.yml
  db/migration/V1__schema.sql
  db/migration/V2__seed_core.sql
frontend/                        # Vite + Vue3 + TS
```

依赖锁定（父任务调研结论）：Boot 2.7.18、MyBatis-Plus 3.5.3.2、JJWT 0.11.5、springdoc 1.6.15、POI 5.2.3、PDFBox 2.0.29、Lombok 1.18.30。

## 2. 数据库

### V1 表

```sql
sys_group(id, name, code, parent_id, leader, sort, enabled, deleted, created_at, updated_at)
sys_user(id, username, password_hash, real_name, email, phone, group_id, status,
         fail_count, locked_until, last_login_at, deleted, created_at, updated_at)
sys_role(id, code, name, description)
sys_user_role(id, user_id, role_id)
tech_direction(id, code, name, parent_id, keywords, sort, enabled, deleted,
               created_at, updated_at)
audit_log(id, user_id, username, action, resource_type, resource_id, detail,
          ip, result, cost_ms, created_at)
```

- 不使用数据库 JSON 类型；`keywords` 存逗号分隔字符串。
- 唯一索引：`sys_user(username)`、`sys_role(code)`、`sys_group(code)`、`tech_direction(code)`。
- 普通索引：`sys_user(group_id, status)`、`audit_log(created_at)`、`audit_log(user_id)`。
- 时间默认在应用层写入，避免 `DEFAULT CURRENT_TIMESTAMP` 方言差异。

### 种子

- 角色：`ADMIN/PMO/GROUP_USER/EXPERT`。
- 管理员：`admin / Admin@123`（BCrypt 哈希写入脚本）。
- 演示账号：`pmo01 / Pmo@12345`、`user01 / User@12345`（属“智能计算项目组”）、`expert01 / Expert@12345`。
- 8 个项目组：智能计算、数据治理、网络安全、嵌入式软件、工业软件、通信网络、系统集成、前沿技术。
- 12 个技术方向：人工智能、大数据、云计算、边缘计算、网络安全、密码技术、嵌入式、工业软件、数字孪生、通信协议、系统集成、可靠性工程；每个含 3–6 个关键词。

## 3. 认证与鉴权

```text
Login -> AuthenticationManager -> UserDetailsService
      -> BCrypt 校验 + 锁定检查
      -> JwtTokenProvider.create(userId, username, roles)
      -> LoginResponse{token, expiresAt, user, roles, permissions}

Request -> JwtAuthenticationFilter
        -> 解析 Bearer -> 校验签名/过期 -> 加载 LoginUser -> SecurityContext
        -> 无 token 或无效: 401 JSON
        -> 角色不匹配: 403 JSON
```

- `SecurityConfig`：无状态 session、禁用 CSRF、放行 `/api/v1/auth/login`、`/api/v1/health`、`/swagger-ui/**`、`/v3/api-docs/**`、静态资源。
- `LoginUser` 实现 `UserDetails`，携带 `userId/username/realName/groupId/roles`。
- 数据范围：`DataScopeService.currentGroupId()`；`GROUP_USER` 查询自动追加 `group_id = current`，`ADMIN/PMO/EXPERT` 按业务规则放行。本任务先提供工具与拦截基础，项目查询在 declaration 接入。
- 密码策略：长度 ≥8，至少一个字母与一个数字；BCrypt strength 10。
- 锁定：`fail_count >= 5` 且 `locked_until > now` 时拒绝登录并提示剩余分钟；成功登录清零。

## 4. 后端接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/v1/auth/login` | 匿名 | 登录 |
| POST | `/api/v1/auth/logout` | 登录 | 写审计，前端清 token |
| GET | `/api/v1/auth/me` | 登录 | 当前用户 |
| PUT | `/api/v1/auth/password` | 登录 | 修改密码 |
| GET | `/api/v1/health` | 匿名 | 健康检查 |
| GET/POST/PUT/DELETE | `/api/v1/groups` | ADMIN | 项目组（首版仅 GET；写接口后置） |
| GET/POST/PUT/DELETE | `/api/v1/users` | ADMIN | 用户（首版仅 GET /me；管理接口后置） |
| POST | `/api/v1/users/{id}/reset-password` | ADMIN | 重置密码（后置） |
| GET | `/api/v1/roles` | ADMIN | 角色列表（首版可只读或省略） |
| GET/POST/PUT/DELETE | `/api/v1/tech-directions` | ADMIN | 技术方向（首版仅 GET；写接口后置） |
| GET | `/api/v1/dicts` | 登录 | 字典聚合 |
| GET | `/api/v1/audit-logs` | ADMIN | 审计日志（后置） |

- 所有写接口 `@Audit`；请求/响应 DTO 与实体分离。
- 校验：`@Valid` + 自定义唯一性校验；删除前检查引用。
- 错误码：`AUTH_BAD_CREDENTIALS(40101)`、`AUTH_LOCKED(40102)`、`AUTH_TOKEN_INVALID(40103)`、`FORBIDDEN(40301)`、`GROUP_HAS_USERS(40011)` 等。

## 5. 前端

```text
src/
  main.ts, App.vue
  router/index.ts             # 静态路由 + meta.roles + 守卫
  stores/auth.ts, dict.ts
  api/http.ts, auth.ts, system.ts
  layouts/MainLayout.vue
  views/login/LoginView.vue
  views/system/{UserView,GroupView,TechDirectionView,AuditLogView}.vue
  views/error/{403,404}.vue
  views/dashboard/DashboardView.vue   # 占位，后续替换
  components/{AppEmpty,AppPageHeader,RoleTag}.vue
  styles/tokens.css, index.css
```

- Vite 代理：`/api` → `http://localhost:8080`。
- 菜单配置集中在 `router` 的 `meta`，按角色过滤。
- 401 清除 token 并跳转 `/login?redirect=...`；403 跳 `/403`。
- 表格统一：筛选区 + 工具栏 + 分页；表单使用 `el-form` 校验。
- 设计 token：主色 `#1f5eff` 系、辅助青 `#00c2b8`、背景 `#f5f7fb`、暗色侧栏 `#0f1b33`；字号 12/14/16/20/28；圆角 6/10。
- 不引入未使用的大型组件库；图表库在后续子任务引入。

## 6. 配置

`application.yml`：

```yaml
spring:
  profiles:
    active: demo
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 25MB
server:
  port: 8080
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
app:
  jwt:
    secret: ${JWT_SECRET:}
    expire-hours: 8
  upload:
    dir: ${UPLOAD_DIR:./storage}
```

- demo：H2 文件库 + Flyway + 固定开发 JWT 密钥（启动 WARN）。
- prod：MySQL + `JWT_SECRET` 必填（缺失启动失败）+ 关闭 SQL 日志。

## 7. 测试

- `AuthServiceTest`：密码错误、锁定、成功登录。
- `SecurityIntegrationTest`（MockMvc）：匿名 401、角色不足 403、正常 200。
- `DataScopeTest`：GROUP_USER 只返回本组。
- `AuditAspectTest`：注解触发、脱敏、异常仍记录。
- `FlywaySmokeTest`：H2 启动并校验种子数量。

## 8. 风险

- Maven 3.5.0 与 Boot 2.7.18：如插件解析失败，升级 Maven 或降级 Boot 2.6.15；本任务内一次性锁定。
- H2 `MODE=MySQL` 对 `AUTO_INCREMENT`、`TEXT`、索引语法支持良好；避免 `JSON`、`FULLTEXT`、`ON UPDATE CURRENT_TIMESTAMP`。
- 旧 `HelloWorld` 类保留或删除需在实施时决定；删除前确认无引用（当前无）。

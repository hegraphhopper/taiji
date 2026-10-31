# 平台底座与权限体系（PRD）

## Goal

把现有 HelloWorld Maven 骨架升级为可运行的自投项目管理平台底座（首版）：后端工程、双数据库配置、最小登录与 JWT、角色/数据范围基础设施、组织与技术方向种子、审计写入、前端应用壳。首版优先支撑“结构化申报书 → 重复度分析 → 趋势归因”主线；完整角色管理、系统管理界面与审批流后置到 `fund-governance`。

## 范围

- 后端：Spring Boot 单体、统一响应/异常、Flyway、MyBatis-Plus、Spring Security + JWT、OpenAPI、健康检查。
- 数据库：`sys_group`、`sys_user`、`sys_role`、`sys_user_role`、`tech_direction`、`audit_log` 及种子数据。
- 前端：Vite + Vue 3 + TS + Element Plus + Pinia + Router 骨架，登录页、主布局、路由守卫、403/404、字典缓存。
- 首版只实现：登录/登出/当前用户/修改密码、字典只读、组织与用户种子、审计写入。
- 移交 `fund-governance`：用户/角色/项目组/技术方向的完整管理界面、细粒度权限矩阵、审计查询、专家评审与审批流。
- 不做：项目、申报书、分析、看板（由后续子任务实现）。

## 功能需求

### F1 工程骨架

- 根 `pom.xml` 改为 `com.taiji:fund-platform:1.0.0`，保留 Java 8 编译配置。
- 引入 Web、Validation、AOP、Security、MyBatis-Plus、MySQL、H2、Flyway、JJWT、springdoc、Lombok、POI/PDFBox（后两者本任务仅锁定版本，供后续使用）。
- `demo` / `prod` 两个 profile；所有敏感配置支持环境变量覆盖。
- 包根 `com.taiji.fund`，按 feature 分包；`FundApplication` 为启动类。
- 统一响应 `Result<T>`、分页 `PageResult<T>`、业务异常 `BizException`、全局异常处理、错误码枚举。
- 健康检查 `GET /api/v1/health` 返回应用名、版本、profile、数据库连通状态。

### F2 数据层

- Flyway `V1__schema.sql` 建系统表，`V1__seed.sql`（或 `V2__seed_core.sql`）写种子：4 个角色、1 个管理员、8 个项目组、12 个技术方向。
- 表结构遵循父任务设计：逻辑删除、时间戳、索引。
- MyBatis-Plus 配置：分页插件、逻辑删除、乐观锁、驼峰映射、SQL 日志仅在 dev/demo 开启。
- 事务边界在 Service 层，只读查询标注 `@Transactional(readOnly = true)`。

### F3 认证

- `POST /api/v1/auth/login`：用户名 + 密码，返回 JWT、用户信息、角色、项目组、菜单权限。
- `POST /api/v1/auth/logout`：无状态登出（前端清 token），写审计。
- `GET /api/v1/auth/me`：当前用户信息。
- `PUT /api/v1/auth/password`：旧密码校验 + 新密码强度校验（≥8 位，含字母与数字）。
- JWT 有效期 8 小时；密钥来自 `JWT_SECRET`，缺失时 demo 使用固定开发密钥并在启动日志告警，prod 缺失直接启动失败。
- 登录失败 5 次锁定 10 分钟；锁定状态与失败计数可持久化。

### F4 角色与数据范围（基础设施）

- 角色枚举与种子固定：`ADMIN`、`PMO`、`GROUP_USER`、`EXPERT`；首版所有写操作可由 `ADMIN` 完成，`PMO/GROUP_USER` 基础登录可用。
- 建立 `SecurityContext`、当前用户工具、`DataScopeService` 钩子与 `@PreAuthorize` 骨架；项目等业务数据范围在后续子任务接入。
- 完整权限矩阵、角色分配界面、禁用/锁定管理后置到 governance。
- 前端路由守卫 + 菜单按角色渲染（首版菜单仅首页与占位）；后端仍必须独立校验。

### F5 种子与字典（只读）

- 种子：8 个项目组、12 个技术方向、4 个角色、管理员与 3 个演示账号。
- `GET /api/v1/dicts` 返回基金类型、项目状态、角色、技术方向，供前端缓存。
- `GET /api/v1/groups`、`/tech-directions` 只读列表（供项目表单选择）；增删改后置到 governance。
- 用户/项目组/技术方向的完整管理界面与接口移交 governance。

### F6 审计日志（写入）

- 注解 `@Audit(action, resourceType)` + AOP 切面，记录用户、动作、资源 ID、请求摘要、IP、结果、耗时。
- 首版覆盖：登录/登出、密码修改、后续业务写操作接入。
- 审计查询接口与页面后置到 governance；日志不可删除或修改，敏感字段必须脱敏。

### F7 前端应用壳

- 登录页：账号密码、错误提示、加载态、演示账号提示（demo profile）。
- 主布局：左侧菜单（按角色）、顶部面包屑/用户菜单、内容区；窄屏可折叠。
- 路由守卫：未登录跳登录并记录 redirect；无权限跳 403；刷新后恢复用户信息。
- Axios 拦截器：注入 Bearer、统一错误提示、401 跳登录、403 跳无权限。
- 状态：`auth` store 持久化 token 与用户；`dict` store 缓存字典。
- 页面：首页占位（后续替换）、403、404；用户/项目组/技术方向/审计页面后置到 governance。
- 设计基调：科技蓝 + 深空灰、数据密集型、克制的动效；遵循项目现有设计 token（无则新建）。

### F8 工程可维护性

- OpenAPI：`/swagger-ui.html` 可访问，接口分组。
- 单测：认证、权限、数据范围、密码策略、审计切面。
- `.gitignore` 覆盖 `target/`、`node_modules/`、`dist/`、`data/`、`storage/`、`.env`。
- `README` 初版：环境要求、启动命令、演示账号、目录说明（交付子任务再扩充）。

## 验收标准

- [x] `mvn -q clean package` 成功，产物可 `java -jar` 以 demo profile 启动。
- [x] Flyway 在 H2 上自动建表并写入种子；重复启动不重复插入。
- [x] 管理员 `admin / Admin@123` 登录成功，返回 JWT 与角色；错误密码提示明确；5 次失败后锁定。
- [x] 未带 token 访问受保护接口返回 401；`GROUP_USER` 访问仅管理员接口返回 403。
- [x] 项目组/技术方向只读接口返回种子数据，可供项目表单直接使用。
- [x] 审计日志写入生效：登录可查到 LOGIN 记录，密码字段不出现明文（查询接口后置）。
- [x] 前端 `npm run build` 通过；登录 → 首页可操作；刷新保持登录；无权限路由显示 403。
- [x] 前端控制台无报错；404 页面可访问。
- [x] `mvn test` 通过，覆盖认证、鉴权基础设施、数据范围钩子、审计写入。
- [x] 仓库无硬编码生产密钥。

## 依赖与约束

- 无前置子任务；必须先于 declaration/engine 完成。
- MySQL 8 脚本需在 WeCode 环境验证；本任务以 H2 验收为准。
- 种子账号仅用于演示，README 明示首次登录后修改密码。
- 完整角色管理、系统管理界面、审计查询移交 `fund-governance`；本任务只保证基础设施不返工。

## 澄清记录

- [x] 认证方式：本地账号 + JWT（依据：父 PRD Q5 确认）。
- [x] 角色：ADMIN/PMO/GROUP_USER/EXPERT（依据：父 PRD 角色表）。
- [x] 数据库：demo H2 / prod MySQL（依据：父 PRD Q1）。

---
task_id: fund-foundation
result: passed
---
# 检查报告

## 检查范围

- 本轮：仅 A1 前端 `frontend/`（`src/`、`index.html`、`vite.config.ts`、`tsconfig.json`、`package.json`）。未修改后端、未修改 `.trellis/spec`、未编写单元测试。
- 对照依据：`prd.md`、`design.md`、`implement.md`、`.trellis/spec/frontend/{quality,component,state-management,type-safety,directory-structure}.md`，以及后端 DTO（`Result`、`LoginResponse`、`UserInfoVO`、`DictVO`、`GroupVO`、`TechDirectionVO`、`ErrorCode`）。
- 受检文件：前端 20 个源文件（4 个 API/store、router、2 个 store、4 个视图、布局、样式、类型、工具）+ 6 个工程配置文件。
- 历史：A1 后端检查记录保留在文末。

## 命令与结果

| 命令 | 工作目录 | 退出码 | 说明 |
|------|----------|--------|------|
| `npm.cmd run typecheck` | `frontend/` | 0 | `vue-tsc --noEmit`，strict 通过 |
| `npm.cmd run build` | `frontend/` | 0 | `vue-tsc --noEmit && vite build`，1689 modules，产物写入 `dist/`（已 gitignore） |
| `Invoke-WebRequest http://localhost:8080/api/v1/health` | 仓库根 | 0（HTTP 200） | demo profile、db UP |
| `Invoke-WebRequest http://localhost:5173/` | 仓库根 | 0（HTTP 200） | Vite dev server 可用 |
| Playwright 交互回归（登录/刷新/无效 token/403/404/登出） | 浏览器 | 全部断言通过 | 详见「复测与证据」 |
| `git check-ignore -v frontend/node_modules frontend/dist data storage .env` | 仓库根 | 0 | 5 项均被 `.gitignore` 命中 |

## 问题及修复

### 中

1. `src/views/login/LoginView.vue` — 密码框同时绑定 `@keyup.enter="submit"`，而 `el-form` 已有 `@submit.prevent="submit"`；在密码框回车会触发两次提交，产生重复登录请求与竞态。快速双击登录按钮同样可重复提交（`submitting` 在 `validate()` 之后才置位）。
   - 修复：移除 `@keyup.enter`（依赖表单原生提交），并将 `submitting` 前置为入口守卫（`if (submitting.value) return`，进入即置 true，`finally` 复位）。
2. `src/router/index.ts` — `to.name === 'login'` 分支里 `fetchMe()` 与 `dict.load()` 位于同一个 `try`，字典接口失败会被当成鉴权失败执行 `auth.logout()`，把有效登录态清掉并把用户踢回登录页。
   - 修复：拆分两个 `try`；仅 `fetchMe` 失败时清理会话，字典失败仅忽略（页面内可重试）。会话清理改用新增的 `auth.clearSession()`，不再对已失效 token 调用 `POST /auth/logout`。
3. `src/layouts/MainLayout.vue` — 修改密码弹窗的 `el-form` 未绑定提交处理，回车可能触发浏览器原生表单提交导致整页刷新；`submitPassword` 也无重复提交守卫。
   - 修复：`el-form` 增加 `@submit.prevent="submitPassword"`，并把 `passwordSubmitting` 前置为入口守卫。

### 低

4. `src/api/http.ts` — 多个并发 401（如刷新时 `me` 与 `dict` 同时失败）会重复改写 `location`；错误消息在无响应体时直接暴露英文 `error.message`。
   - 修复：新增 `redirectingToLogin` 去重标记；抽出 `resolveErrorMessage()`，按「服务端 message → 超时 → HTTP 状态 → 网络异常」顺序给出可读中文，401/403 仍分别跳登录页/403 页且避开当前页造成循环。
5. `src/stores/dict.ts` — 路由守卫与 `MainLayout.onMounted` 可能同时触发字典加载，缺少在途请求去重，会发出重复请求。
   - 修复：增加模块内 `inflight` Promise，复用同一在途请求；`force` 刷新时若已有在途请求也复用，避免互相覆盖。
6. `src/utils/redirect.ts`（新增） — 登录后直接使用 `redirect.startsWith('/')` 校验，`//other-host`、`/\/` 形式也可通过。
   - 修复：新增 `safeRedirect()`，拒绝非站内绝对路径、协议相对地址与 `/login`、`/403` 回跳目标；`LoginView` 与路由守卫统一使用。
7. `src/router/index.ts` — `RouteMeta.permission` 声明后从未在守卫中校验，等于无路由级鉴权骨架。
   - 修复：改为 `RouteMeta.roles?: string[]`，守卫在恢复用户后校验 `auth.hasRole(...to.meta.roles)`，不满足跳 `/403`（当前无受 roles 限制的路由，不影响现有页面）。
8. `src/layouts/MainLayout.vue` — 仅图标的折叠按钮无无障碍名称。
   - 修复：补充 `aria-label`/`title`「切换侧边栏」。

## 复测与证据

- 第 1 轮（静态）：修改后 `npm.cmd run typecheck` 退出码 0；`npm.cmd run build` 退出码 0。
- 第 2 轮（运行态，Playwright + 正在运行的 Vite/后端，未启动或停止任何服务）：
  - 登录：`admin / Admin@123` 提交后跳转 `/dashboard`，工作台显示「系统管理员」、技术方向 12、角色 4、基金类型 3，字典接口数据已渲染。
  - 错误密码：仅提交一次错误密码，页面出现 `el-alert`「用户名或密码错误」；随后正确密码登录成功，失败计数被清零（未触发锁定）。
  - 刷新保持：`/dashboard` reload 后仍停留 `/dashboard`，`/auth/me` 恢复用户信息（body 含「系统管理员」）。
  - 已登录访问 `/login`：跳回 `/dashboard`。
  - 无效 token：写入 `invalid.token.value` 后访问 `/dashboard`，被重定向到 `/login?redirect=%2Fdashboard`，`localStorage` token 已清除。
  - 无权限页：`/403` 渲染「没有访问权限」；未知路径渲染「页面不存在」。
  - 登出：用户菜单「退出登录」→ 确认后跳 `/login` 且 token 清除；再次登录成功。
  - 布局：1440×900 与 900×800 下 `scrollWidth === innerWidth`，无横向溢出；截图见 `D:/LLMAgent/deliverables/Wecode-功能测试离线依赖-v1/artifacts/dashboard-1440.png`、`dashboard-900.png`（当前模型不支持图像输入，未完成目测审美审查，仅以 DOM 尺寸断言 + 截图留证）。
  - 控制台：无 JS 异常/Vue 警告；仅浏览器网络层记录负向用例的 401（`/auth/me`、`/auth/login`），属预期。
- 接口契约核对：前端 `ApiResult`/`UserInfo`/`LoginResponse`/`Dicts`/`TechDirection`/`GroupItem` 与后端 VO 字段（含 `parentId` 可空、`keywords` 为数组）一致，无 `any`/`as any`；未使用 `v-html`/`innerHTML`。

## 未执行项及原因

- 单元测试：跳过原因：本次检查范围明确不写单元测试，前端单测由后续测试阶段安排；`test/unit/report-round-*.md` 本轮不产出。
- `GROUP_USER` 访问仅管理员接口返回 403 的前端表现：跳过原因：A1 首版无管理员专属页面/接口调用入口，前端无法构造该场景；后端权限矩阵已在后端检查阶段由单测/接口覆盖计划承接。
- 修改密码弹窗的全链路提交（含改密后跳登录）：跳过原因：会修改运行中 demo 环境的 admin 密码，影响后续验收，仅验证了表单 Enter 提交与守卫代码路径。
- 窄屏（<640px）与手机端截图：跳过原因：任务验收明确 900 与 1440 两档，本次按该范围验证。
- 目测视觉审查：跳过原因：当前模型不支持图像输入，无法读取已保存的截图，按规范标注未完成。

## 未修复项与风险

- 登录页硬编码演示账号与 `admin / Admin@123` 明文提示，且不区分 demo/prod 构建。属 PRD F7「演示账号提示（demo profile）」要求，但当前没有前端可用的 profile 标识；若生产构建直接复用该页面，会暴露种子账号口令，交付前建议由后端暴露 profile 或改用 `VITE_*` 开关控制显示。
- token 存于 `localStorage`，存在 XSS 读取风险；A1 采用无状态 JWT + 本地持久化是既定方案，后续需靠严格 CSP 与输出转义降低风险。
- `permissions` 仅来自登录响应，刷新后为空；因此本轮路由守卫用 `roles` 做路由级校验。若 governance 阶段要用 `permission` 做菜单/路由控制，需要 `/auth/me` 返回权限或改为按角色派生。
- 侧边栏在 <900px 只能手动折叠（点击折叠按钮），没有抽屉式自动收起；900px 档无溢出，但极窄屏体验需在后续完善。
- 401 采用 `window.location.href` 整页跳转（会丢失 SPA 状态），换取实现简单与彻底清理；若后续需要保留未提交表单，应改为路由跳转 + store 清理。
- 工作台「基础数据-项目组 8」仍是种子常量展示，字典接口未返回项目组数量，数据变化后会失真（A2 接入真实统计时应替换）。

## 历史记录：A1 后端检查（result: passed）

- 命令：`mvn -q -DskipTests compile` 退出码 0；`mvn -q -DskipTests test-compile` 退出码 0；`scripts/*.ps1` Parser 解析 OK。
- 已修复高优先级：登录失败计数因事务回滚不落库（新增 `LoginAttemptService` REQUIRES_NEW）；`stop-demo.ps1` 误杀非本项目进程（增加命令行归属校验）。
- 已修复中/低优先级：PID 复用误判、锁定到期计数未重置、`demo,prod` 同时激活绕过 prod 强校验、demo 配置写死 JWT 密钥、非法 JSON 请求体语义、审计切面 argNames、登录入参长度上限。
- 后端未执行项：`mvn clean package`（jar 运行占用）、真实登录/锁定运行态验收（由主会话/前端运行态验收覆盖）、单元测试（后续测试阶段）、MySQL 实库（无环境）。

## 单元测试 Round 1（result: failed，通过率 95.45% < 阈值 100%）

- 报告：`.trellis/tasks/10-29-fund-foundation/test/unit/report-round-1.md`
- 新建测试源码（7 个文件，22 个测试方法，覆盖 UT-01 ~ UT-20）：
  - `src/test/java/com/taiji/fund/AbstractApiTest.java`（Spring Boot + H2 内存库 + MockMvc 基类，唯一用户名造数隔离）
  - `src/test/java/com/taiji/fund/security/AuthServiceTest.java`（UT-01/02/03/04/13/14/20）
  - `src/test/java/com/taiji/fund/security/SecurityIntegrationTest.java`（UT-05/06/07/08/09/19）
  - `src/test/java/com/taiji/fund/security/DataScopeServiceTest.java`（UT-18）
  - `src/test/java/com/taiji/fund/audit/AuditLoggingTest.java`（UT-12）
  - `src/test/java/com/taiji/fund/web/ReadOnlyApiTest.java`（UT-10/11/16）
  - `src/test/java/com/taiji/fund/bootstrap/SeedDataTest.java`（UT-15）
  - `src/test/java/com/taiji/fund/db/FlywayMigrationTest.java`（UT-17）
  - `src/test/java/com/taiji/fund/support/AdminProbeController.java`（测试专用 `@PreAuthorize("hasRole('ADMIN')")` 探针，不进入生产代码）
- 命令与结果：`mvn -q test`（仓库根）退出码 1；`Tests run: 22, Failures: 1, Errors: 0, Skipped: 0`。未执行 `mvn clean` / `mvn package`，未启动或停止任何服务。
- 失败用例：UT-11（`ReadOnlyApiTest.java:79`）——`POST /api/v1/groups` 期望 405/403，实际 500。根因为 `GlobalExceptionHandler#handleException(Exception.class)` 兜底捕获 `HttpRequestMethodNotSupportedException` 并映射为 `INTERNAL_ERROR(50000)`；本轮不修改生产代码。
- 其它记录（非失败项）：`design.md` 第 3 节写 `ADMIN/PMO/EXPERT` 放行，实现 `DataScopeService#isUnrestricted` 仅放行 `ADMIN/PMO`（测试按用例设计 UT-18 断言）；UT-08 的"审计记录拒绝结果"未断言（读接口/探针无 `@Audit`，仅写接口要求审计）。
- 结论：未达 `test_pass_threshold=1.0`，需派发 `trellis-test-rework` 进入修复轮；修复后应重新运行 `mvn -q test` 并更新本报告 result 与本节结论。

## 单元测试 Round 2（result: passed，通过率 100% = 阈值 100%）

- 报告：`.trellis/tasks/10-29-fund-foundation/test/unit/report-round-2.md`
- 命令：`mvn -q test`（仓库根）→ 退出码 0；surefire 汇总 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`；7 个测试类全部通过。
- 修复（产品代码，测试断言零改动）：
  - `src/main/java/com/taiji/fund/common/exception/ErrorCode.java:22-26`：新增 `METHOD_NOT_ALLOWED(40500)`、`UNSUPPORTED_MEDIA_TYPE(41500)`，复用 `NOT_FOUND(40400)`。
  - `src/main/java/com/taiji/fund/common/exception/GlobalExceptionHandler.java:57-80`：新增 `HttpRequestMethodNotSupportedException → 405`、`HttpMediaTypeNotSupportedException → 415`、`NoHandlerFoundException → 404` 显式处理，使其优先于 `handleException(Exception.class)` 兜底，修复 Round 1 UT-11 的 500 问题。
- 复测：UT-11 `POST /api/v1/groups`、`PUT /api/v1/tech-directions` 均返回 405，断言通过；其余 19 条用例回归通过。
- 证据：`target/surefire-reports/*.txt`（7 个）。
- 结论：Round 2 达标（22/22，100%），原 `failed`（Round 1）已由本轮结果覆盖；未执行 `mvn clean package`（jar 运行占用），未启动/停止服务，未提交 git。

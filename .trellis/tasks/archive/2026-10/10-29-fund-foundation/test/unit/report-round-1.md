# 单元测试报告 Round 1

## 执行信息

- 执行命令：`mvn -q test`（工作目录：仓库根；退出码 `1`，仅因 1 条断言失败，编译与其余用例正常）
- 测试环境：Spring Boot Test + H2 内存库（`MODE=MySQL`，`spring.datasource.url=jdbc:h2:mem:fundtest-${random.uuid}`）+ MockMvc，`@ActiveProfiles({"demo","test"})`；不连接 `data/` 文件库，未启动/停止 8080、5173 服务
- 用例总数：22 个测试方法，覆盖用例设计 UT-01 ~ UT-20 全部 20 条用例（UT-18 拆为 3 个方法）｜通过：21｜失败：1｜跳过：0
- 通过率：95.45%｜阈值：100%｜轮次：1/5

### 用例 → 测试方法映射

| 用例 | 测试类#方法 | 结果 |
|------|-------------|------|
| UT-01 | `AuthServiceTest#ut01_loginSucceedsAndIssuesToken` | 通过 |
| UT-02 | `AuthServiceTest#ut02_wrongPasswordRejectedAndFailureCounted` | 通过 |
| UT-03 | `AuthServiceTest#ut03_fiveFailuresLockAccount` | 通过 |
| UT-04 | `AuthServiceTest#ut04_disabledUserCannotLogin` | 通过 |
| UT-05 | `SecurityIntegrationTest#ut05_validJwtAuthenticatesRequest` | 通过 |
| UT-06 | `SecurityIntegrationTest#ut06_invalidOrExpiredJwtRejected` | 通过 |
| UT-07 | `SecurityIntegrationTest#ut07_anonymousProtectedRequestRejected` | 通过 |
| UT-08 | `SecurityIntegrationTest#ut08_groupUserForbiddenOnAdminEndpoint` | 通过 |
| UT-09 | `SecurityIntegrationTest#ut09_currentUserPayloadIsCompleteAndSafe` | 通过 |
| UT-10 | `ReadOnlyApiTest#ut10_dictsAggregateSeedData` | 通过 |
| UT-11 | `ReadOnlyApiTest#ut11_groupAndTechDirectionListsAreReadOnly` | **失败** |
| UT-12 | `AuditLoggingTest#ut12_loginAndLogoutAreAuditedWithMaskedSecrets` | 通过 |
| UT-13 | `AuthServiceTest#ut13_changePasswordWithWrongOldPassword` | 通过 |
| UT-14 | `AuthServiceTest#ut14_weakNewPasswordRejected` | 通过 |
| UT-15 | `SeedDataTest#ut15_demoAccountsInitializedFromInitPlaceholder` | 通过 |
| UT-16 | `ReadOnlyApiTest#ut16_healthEndpointReportsUp` | 通过 |
| UT-17 | `FlywayMigrationTest#ut17_seedDataIsCompleteAndMigrationIsIdempotent` | 通过 |
| UT-18 | `DataScopeServiceTest#ut18_*`（3 个方法） | 全部通过 |
| UT-19 | `SecurityIntegrationTest#ut19_logoutIsStateless` | 通过 |
| UT-20 | `AuthServiceTest#ut20_failureMessageDoesNotRevealAccountExistence` | 通过 |

## 失败用例明细

| 用例ID | 期望 | 实际 | 原因/堆栈摘要 |
|--------|------|------|---------------|
| UT-11（写接口未暴露部分） | `POST /api/v1/groups`、`PUT /api/v1/tech-directions` 返回 **405 或 403** | `POST /api/v1/groups` 返回 **500**（体为 `INTERNAL_ERROR(50000)`） | 断言位置 `ReadOnlyApiTest.java:79`：`Expecting actual: 500 to be in: [403, 405]`。根因：`GlobalExceptionHandler` 的 `@ExceptionHandler(Exception.class)` 兜底捕获了 Spring MVC 抛出的 `org.springframework.web.HttpRequestMethodNotSupportedException: Request method 'POST' not supported`，将其映射为 500，而不是交给 `DefaultHandlerExceptionResolver` 产出 405。日志证据：`ERROR c.t.f.c.e.GlobalExceptionHandler : 未处理的服务器异常 ... Type = org.springframework.web.HttpRequestMethodNotSupportedException` |

失败的可读断言仅此一处；同一方法中 `PUT /api/v1/tech-directions` 因前一断言失败未被执行，按代码路径推断为同一根因（`HttpRequestMethodNotSupportedException`），修复轮需一并复测。

## 修复说明

本轮仅修复测试代码自身的编译/编码问题，未削弱任何断言、未使用 skip/only、未修改生产代码：

- `src/test/java/com/taiji/fund/bootstrap/SeedDataTest.java:52`：`ApplicationRunner#run` 声明 `throws Exception`，测试方法签名补 `throws Exception`（编译错误修复）。
- `src/test/java/com/taiji/fund/security/AuthServiceTest.java:76-81`：`for` 循环计数变量在 lambda 中被捕获，改为 `final String wrong = "bad-password-" + i`（编译错误修复，断言逻辑不变）。
- `src/test/java/com/taiji/fund/security/DataScopeServiceTest.java`：删除辅助方法中无意义的自证断言与多余 import（清理，不影响断言强度）。

## 生产代码疑似缺陷（交回主会话，本轮不修改生产代码）

1. **框架类异常被兜底为 500**（阻塞 UT-11）：
   - 位置：`src/main/java/com/taiji/fund/common/exception/GlobalExceptionHandler.java` 的 `handleException(Exception.class)`。
   - 现象：任何未显式声明的 Spring MVC 标准异常（405 `HttpRequestMethodNotSupportedException`、415 `HttpMediaTypeNotSupportedException`、404 `NoHandlerFoundException` 等）都会被映射成 `50000 INTERNAL_ERROR`，与其 `ResponseStatusException`/`ErrorResponse` 语义不符；前端无法区分"方法不允许"和"服务端故障"。
   - 建议（供修复轮参考）：为 `HttpRequestMethodNotSupportedException`（405）、`HttpMediaTypeNotSupportedException`（415）、`NoHandlerFoundException`/`NoResourceFoundException`（404）增加显式处理，或让 `handleException` 排除这些子类；并在 `ErrorCode` 中补充对应错误码。
2. **design.md 与实现的数据范围范围差异（非失败项，仅记录）**：`design.md` 第 3 节写"`ADMIN/PMO/EXPERT` 按业务规则放行"，而 `DataScopeService#isUnrestricted` 仅放行 `ADMIN`/`PMO`；用例设计 UT-18 明确"`ADMIN/PMO` 不受限"，故测试按实现 + 用例设计断言（`EXPERT` 受限）。后续 declaration 阶段接入业务数据范围前需确认 `EXPERT` 的口径，避免返工。
3. **UT-08 的"审计记录拒绝结果"未纳入断言**：403 由方法级 `@PreAuthorize` 在进入 Controller 前抛出，`@Audit` 切面与安全拦截器的执行顺序不确定，且首版只要求"写接口 @Audit"（读接口与探针接口无 `@Audit`）。因此本轮校验 403 + `FORBIDDEN(40301)` 状态码，`@Audit` 的失败结果记录留待 governance 阶段随审计查询接口一并验证。
4. **UT-19 无状态登出**：实现未做 token 黑名单，登出后同一 JWT 在有效期内仍可访问 `/auth/me`（200），测试按实现断言并接受 200/401 两种合法语义，与 design "无状态登出（前端清 token）"一致，非缺陷。

## 备注

- UT-01 额外覆盖"登录成功清零历史失败计数"（专用账号预置 `fail_count=3`，登录后为 0）；UT-15 通过重新调用 `demoAccountInitializer` 验证重复启动不会重复编码。
- 所有 Spring 测试共享同一 `ApplicationContext` 与同一 H2 内存库，用户数据统一用"前缀 + 序号 + nanoTime"唯一用户名创建，锁定/禁用/改密用例均使用专用账号，不污染 `admin`、`user01` 等种子账号；种子稳定性由 `FlywayMigrationTest` 独立内存库验证。
- 报告与执行产物目录：`.trellis/tasks/10-29-fund-foundation/test/unit/`；失败明细原始证据见 `target/surefire-reports/com.taiji.fund.web.ReadOnlyApiTest.txt`。

## 剩余失败

- UT-11 仍失败（生产代码 405 语义缺失），本轮不进入修复轮。**通过率 95.45% < 阈值 100%，未达标，需由主会话派发 `trellis-test-rework` 进入修复轮。**

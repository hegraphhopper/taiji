# 单元测试报告 Round 2

## 执行信息

- 执行命令：`mvn -q test`（工作目录：仓库根；退出码 `0`，未执行 `clean`/`package`，未启动或停止任何服务）
- 测试环境：Spring Boot Test + H2 内存库（`MODE=MySQL`）+ MockMvc，`@ActiveProfiles({"demo","test"})`；沿用 Round 1 的测试基类与造数隔离策略，未改动测试断言
- 用例总数：22 个测试方法，覆盖用例设计 UT-01 ~ UT-20 全部 20 条用例（UT-18 拆为 3 个方法）｜通过：22｜失败：0｜跳过：0
- 通过率：100%｜阈值：100%｜轮次：2/5

### 各测试类结果（surefire 汇总）

| 测试类 | 用例数 | 失败 | 错误 | 跳过 |
|--------|--------|------|------|------|
| `AuthServiceTest` | 7 | 0 | 0 | 0 |
| `SecurityIntegrationTest` | 6 | 0 | 0 | 0 |
| `DataScopeServiceTest` | 3 | 0 | 0 | 0 |
| `ReadOnlyApiTest` | 3 | 0 | 0 | 0 |
| `AuditLoggingTest` | 1 | 0 | 0 | 0 |
| `SeedDataTest` | 1 | 0 | 0 | 0 |
| `FlywayMigrationTest` | 1 | 0 | 0 | 0 |

## 失败用例明细

无。Round 1 唯一失败项 UT-11 已通过：

| 用例ID | 期望 | 实际（Round 2） | 结论 |
|--------|------|-----------------|------|
| UT-11 | `POST /api/v1/groups`、`PUT /api/v1/tech-directions` 返回 405 或 403 | 两者均返回 **405**，响应体为统一 `Result`（`code=40500`、`message=请求方法不支持`） | 通过 |

## 修复说明

修复产品代码缺陷（框架异常被兜底为 500），未修改任何测试断言、未修改 `test/用例设计.md`、未使用 skip/only、未删除断言。

1. `src/main/java/com/taiji/fund/common/exception/ErrorCode.java:22-26`
   - 新增 `METHOD_NOT_ALLOWED(40500, "请求方法不支持", 405)`、`UNSUPPORTED_MEDIA_TYPE(41500, "不支持的请求内容类型", 415)`；`NOT_FOUND(40400, "资源不存在", 404)` 复用已有定义。
   - 原因：框架异常需要明确的业务错误码与 HTTP 状态，避免统一落到 `INTERNAL_ERROR(50000)`。

2. `src/main/java/com/taiji/fund/common/exception/GlobalExceptionHandler.java:12-16`（import）
   - 引入 `HttpRequestMethodNotSupportedException`、`HttpMediaTypeNotSupportedException`、`NoHandlerFoundException`。
   - 原因：需要在兜底 `handleException(Exception.class)` 之前用更精确的 `@ExceptionHandler` 接管这些 Spring MVC 标准异常（`ExceptionHandlerExceptionResolver` 按异常类型相近度匹配，具体类型优先于 `Exception`）。

3. `src/main/java/com/taiji/fund/common/exception/GlobalExceptionHandler.java:57-80`
   - 新增 `handleMethodNotSupported` → HTTP 405 + `Result.fail(ErrorCode.METHOD_NOT_ALLOWED)`；
   - 新增 `handleMediaTypeNotSupported` → HTTP 415 + `Result.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE)`；
   - 新增 `handleNoHandlerFound` → HTTP 404 + `Result.fail(ErrorCode.NOT_FOUND)`（仅在启用 `throw-exception-if-no-handler-found` 时触发，未改变全局配置，不影响现有静态资源/默认 404 行为）。
   - 原因：修复 Round 1 根因——`HttpRequestMethodNotSupportedException` 被 `@ExceptionHandler(Exception.class)` 兜底映射为 `INTERNAL_ERROR(50000)`，导致前端无法区分“方法不允许”和“服务端故障”。修复后 `handleException` 仍保留为真正的未知异常兜底。

## 复测与证据

- 第 1 次执行：`mvn -q test` → 退出码 0；surefire 7 个测试类全部 `Failures: 0, Errors: 0, Skipped: 0`，合计 `Tests run: 22`。
- UT-11 断言 `ReadOnlyApiTest.java:79`、`:86` 均通过（`isIn(403, 405)`），说明只读接口未暴露写方法且不再返回 500。
- 其余 19 条用例未受影响（同一 Spring 上下文、同一 H2 内存库策略），认证、鉴权、数据范围、审计、种子用例全部通过。
- 原始证据：`target/surefire-reports/com.taiji.fund.web.ReadOnlyApiTest.txt` 等 7 个报告文件。

## 备注

- Round 1 报告中的“生产代码疑似缺陷”第 1 条已修复；第 2 条（`EXPERT` 数据范围口径）、第 3 条（UT-08 审计拒绝结果）、第 4 条（无状态登出）非失败项，仍留待 governance 阶段确认，本轮未改动。
- 未执行 `mvn clean package`（后端 jar 正在运行占用，且不属于单元测试范围）；未做前端与 `.trellis/spec` 改动。

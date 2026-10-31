# 后端错误处理（A1 实测）

## 统一响应

```json
{"code": 0, "message": "成功", "data": {}, "traceId": null}
```

- `code=0` 成功；非 0 为业务错误码。
- HTTP 状态与 `ErrorCode.httpStatus` 一致。
- 失败时 `data=null`；对外不返回堆栈、SQL、内部路径。

## 错误码映射

| 错误码 | HTTP | 场景 |
|--------|------|------|
| `40000 BAD_REQUEST` | 400 | 参数错误、请求体 JSON 非法、校验失败 |
| `40012 PASSWORD_WEAK` | 400 | 新密码强度不足 |
| `40100 UNAUTHORIZED` | 401 | 未认证、登录过期 |
| `40101 AUTH_BAD_CREDENTIALS` | 401 | 用户名或密码错误（不区分账号是否存在） |
| `40102 AUTH_LOCKED` | 401 | 连续失败锁定 |
| `40103 AUTH_TOKEN_INVALID` | 401 | JWT 非法/过期 |
| `40104 AUTH_OLD_PASSWORD_INVALID` | 400 | 旧密码错误 |
| `40105 AUTH_DISABLED` | 401 | 账号禁用 |
| `40301 FORBIDDEN` | 403 | 角色或数据范围不足 |
| `40400 NOT_FOUND` | 404 | 资源不存在 |
| `40500 METHOD_NOT_ALLOWED` | 405 | HTTP 方法不支持 |
| `41500 UNSUPPORTED_MEDIA_TYPE` | 415 | Content-Type 不支持 |
| `50000 INTERNAL_ERROR` | 500 | 未预期异常 |

## 处理规则

- 业务错误抛 `BizException(ErrorCode[, message])`，由 `GlobalExceptionHandler` 统一转换。
- 校验错误合并字段消息，返回 400。
- 框架异常必须显式处理：`HttpRequestMethodNotSupportedException → 405`、`HttpMediaTypeNotSupportedException → 415`、`NoHandlerFoundException → 404`；不得被 `Exception.class` 兜底成 500。
- 安全异常：`AccessDeniedException → 403`；`AuthenticationException → 401`。
- 兜底 500 记录 error 日志，但不向客户端泄露异常细节。
- 前端根据 HTTP 状态处理 401/403，业务错误展示 `message`。

## Good / Base / Bad

- Good：只读接口收到 POST 返回 405 JSON；未知异常返回 500 通用消息。
- Base：参数校验失败返回 400 并列出字段消息。
- Bad：所有异常统一 200 或统一 500；把异常堆栈返回前端。

## 测试要求

- 401、403、405、415、400、500 各至少一条断言。
- 断言响应 `code` 与 HTTP 状态，不只看消息文本。
- 断言错误响应不含 `password`、`token`、堆栈关键字。

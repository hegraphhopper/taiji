# 后端日志规范（A1 实测）

## 日志入口

- 使用 SLF4J（`@Slf4j`），不直接使用 `System.out`。
- 启动、迁移、认证失败、审计写入失败、未处理异常记录日志。
- demo profile 的 MyBatis SQL 日志使用 `StdOutImpl`；prod 必须关闭 SQL 日志。

## 脱敏要求

禁止写入日志、错误响应或 URL 的内容：

- 密码、密码哈希、JWT、Authorization 头、API Key、密钥。
- 完整请求体中的敏感字段。
- 附件正文、用户手机号等业务敏感信息。

`AuditAspect` 将 `password`、`token`、`secret`、`credential`、`authorization` 字段统一替换为 `***`；新增敏感字段时同步更新脱敏列表。

## 审计日志

- 写操作使用 `@Audit(action, resourceType)`，记录：用户、动作、资源类型/ID、请求摘要、IP、结果、耗时。
- 审计写入失败只记录 warn，不阻断主流程。
- `audit_log.detail` 最长 1900 字符；超长截断。
- IP 来源当前信任 `X-Forwarded-For`；部署在反向代理后需确认可信代理，避免伪造。生产接入网关时改为只信任网关注入头。
- 审计日志只允许查询，不允许 API 删除或修改（查询接口在 governance 阶段）。

## 日志位置

- `target/app.out.log`、`target/app.err.log`（后端）。
- `target/vite.out.log`、`target/vite.err.log`（前端 dev）。
- 由 `scripts/start-demo.ps1` 重定向；不要在工具调用中前台启动常驻进程。

## 测试要求

- 登录成功/失败、改密、登出均产生审计记录。
- 审计详情不含明文密码或 token。
- 审计写入异常不影响登录返回。

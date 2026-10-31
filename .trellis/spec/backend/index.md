# 后端开发规范

本项目为电科太极自投项目重复投入治理平台，后端 Spring Boot 2.7 单体，Java 8，演示 H2、生产 MySQL 8。

## 规范索引

| 文件 | 用途 |
|------|------|
| [目录结构](./directory-structure.md) | 包结构、分层边界与测试位置 |
| [认证与安全](./security-guidelines.md) | JWT、锁定、数据范围、审计契约 |
| [项目与申报书](./project-guidelines.md) | 项目 CRUD、状态流转、附件解析契约 |
| [数据库规范](./database-guidelines.md) | Flyway、H2/MySQL 兼容、事务与测试库 |
| [错误处理](./error-handling.md) | 统一响应、错误码、框架异常映射 |
| [日志规范](./logging-guidelines.md) | 审计、脱敏与日志位置 |
| [质量规范](./quality-guidelines.md) | 构建/测试命令、运行脚本与常见陷阱 |

## 开发前检查（Pre-Development Checklist）

- 读取任务 `prd.md`、`design.md`、`implement.md` 与相关测试用例。
- 确认现有契约、校验、权限边界与事务边界，复用 `common`、`security` 中的工具。
- 新增接口前确认错误码、HTTP 状态、审计与数据范围要求。
- 涉及数据库改动先检查 H2/MySQL 兼容性。

## 质量检查（Quality Check）

- 运行 `mvn -q test`，确认无失败；涉及前端时运行 `npm.cmd run typecheck` 与 `npm.cmd run build`。
- 运行态用 `scripts/start-demo.ps1` 启动并使用 `scripts/status-demo.ps1` 检查。
- 覆盖正常、边界、拒绝、失败路径；报告实际命令与退出码。
- 规范同步内容使用中文；代码、命令、路径保持原样。

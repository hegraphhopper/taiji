# C1 质量检查结论（check）

## 范围

- 变更：54 paths（治理后端 20 + 前端 8 + 测试 7 + 任务文档 3，另有新增目录）。
- 规范：backend quality/security/database/error-handling、frontend quality/component/state、cross-layer guide。
- 结论：通过；修复 2 项需求偏差后重新全量验证。

## 逐项结论

| 检查项 | 结论 | 证据 |
|--------|------|------|
| 权限三处对齐 | 通过 | 后端 `PermissionCatalog` 与前端 `utils/permission.ts` 22 个权限点角色一致；`router.meta.roles` 与菜单条件一致 |
| 项目动作边界 | 修复后通过 | 提交/撤回/开始/结项仅 ADMIN + 同组 GROUP_USER；PMO 仅审查/立项/驳回/终止；`UT-G14` 断言 PMO 403 |
| PASS 门禁 | 修复后通过 | 全轮次按专家去重；`UT-G06/G07` 覆盖 1 票拦截、2 票立项、旧 PASS 计入 |
| 数据范围 | 通过 | GROUP_USER 本组；EXPERT `REVIEWING` + 本组非草稿；趋势/投入/决策/分析链路均有服务层校验 |
| 审计 | 通过 | 写接口 `@Audit` 全覆盖；`AuditAspect` 敏感字段替换 `***`；查询/导出仅 ADMIN |
| 删除引用校验 | 通过 | 组下有用户/项目、技术方向被引用时 400（`UT-G04`） |
| 数据库迁移 | 通过 | `V6__system_config.sql` 仅用 H2/MySQL 共同语法；H2 全量测试启动迁移通过 |
| 前端质量 | 通过 | `npm run typecheck`、`npm run build` 退出码 0；菜单/按钮/路由按角色收敛 |
| 错误处理 | 通过 | 403/400/401 由统一异常处理返回；无堆栈外泄 |
| 回归 | 通过 | `mvn -q test` 172/172；`mvn -q -DskipTests package` 退出码 0 |

## 发现与修复

1. PASS 门禁只统计当前轮次，与「同一专家多轮 PASS 只计 1 人」不符 → 改为全轮次去重统计，更新 `UT-G07`。
2. PMO 可通过 `isUnrestricted()` 创建/提交/开始/结项项目，超出 PRD F4 动作权限 → 收敛后端访问校验、前端按钮与权限目录，补充 `UT-G14` 断言。

## 遗留

- 浏览器四角色走查与截图见 `report.md`。
- 多节点部署、统一身份对接不在本期范围。

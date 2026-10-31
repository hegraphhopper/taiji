# 角色权限、系统管理与审批流（C1）— 验收报告

## 1. 交付内容

- 权限：`PermissionCatalog` 统一权限目录（22 个权限点）+ 前端 `utils/permission.ts` 同源；后端 `@PreAuthorize` 最终校验。
- 系统管理：用户（新增/编辑/启停/重置密码/角色分配）、项目组、技术方向、角色权限点、系统参数、审计日志。
- 审批流：`SUBMITTED → REVIEWING → APPROVED/REJECTED`、`APPROVED → IN_PROGRESS → COMPLETED/TERMINATED`；形式审查、专家评审（多轮、评分、结论）、立项 PASS 门禁、驳回重提、终止。
- 前端页面：用户/组织字典/角色权限/系统参数/审计日志、评审任务与评审弹窗、待办中心、项目审批轨迹与专家评审页签。
- 数据库：`V6__system_config.sql`（系统参数 + `review_round` + 评审索引）。
- 演示账号：demo profile 增加 `expert02`、`expert03`，满足默认 2 票演示。

## 2. 测试结果

| 项 | 结果 |
|----|------|
| C1 单元用例 | 14/14 通过（UT-G01 ~ UT-G14） |
| 全量回归 | 53 个测试类 / 172 项，Failures 0 / Errors 0 / Skipped 0 |
| 后端构建 | `mvn -q -DskipTests package` 退出码 0 |
| 前端类型检查 | `npm.cmd run typecheck` 退出码 0 |
| 前端构建 | `npm.cmd run build` 退出码 0（仅 chunk 体积告警） |

检查与用例明细见 `check.md`、`test/用例设计.md`、`test/unit/report-round-1.md`。

## 3. 四角色走查（Playwright，1440×900）

| 角色 | 走查结论 | 截图 |
|------|----------|------|
| ADMIN | 系统管理五项、审计查询可用；菜单与接口全量 | `admin-system-org.png`、`admin-system-users.png`、`admin-system-roles.png`、`admin-system-config.png`、`admin-system-audit.png` |
| PMO | 无系统管理/无新建项目入口；可查看全所项目、审批轨迹与专家评审 | `pmo-dashboard.png`、`pmo-project-list.png`、`pmo-project-detail.png`、`pmo-approval-timeline.png`、`pmo-expert-reviews.png` |
| EXPERT | 仅项目、评审任务、待办、决策记录；评审弹窗可评分/结论；访问趋势跳 403 | `expert-dashboard.png`、`expert-review-tasks.png`、`expert-review-dialog.png`、`expert-403-trend.png` |
| GROUP_USER | 项目列表仅本组 8 个；总览、趋势、投入、决策、待办可见；无系统管理/分析运行 | `groupuser-project-list.png`、`groupuser-todo.png` |

截图目录：`docs/screenshots/governance/`，共 16 张，未包含密码、Token 或本机路径。

## 4. 本轮修复

1. **PASS 门禁**：由「仅当前轮次」改为「项目全部轮次按专家去重」，符合「同一专家多轮 PASS 只计 1 人」；`UT-G07` 同步更新。
2. **PMO 越权**：提交/撤回/开始执行/结项收敛为 ADMIN + 同组 GROUP_USER，PMO 仅保留审查/立项/驳回/终止；后端访问校验、前端按钮与权限目录同步修改，`UT-G14` 增加 403 断言。
3. **前端入口**：PMO 项目列表隐藏「新建项目」按钮（`v-if="canCreate"`）。

## 5. 遗留

- WeCode/生产环境部署验证归 D 阶段。
- 统一身份对接、自定义角色、多级会签不在本期范围。

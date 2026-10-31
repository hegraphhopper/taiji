# 角色权限、系统管理与审批流（收尾）— 技术设计

## 1. 复用与边界

- 复用 foundation 的 `SecurityConfig`、`JwtAuthenticationFilter`、`LoginUser`、`DataScopeService`、`@Audit`。
- 复用 declaration 的 `Project`、`ProjectReview`、`ProjectApprovalLog` 表与 `ProjectStateMachine`（首版仅启用部分动作）。
- 本任务主要新增：管理端 Service/Controller、权限注解、评审与审批动作、前端管理页面、审计查询。
- 不新增核心业务表；如需权限点表，使用代码内枚举而非数据库配置，避免运行期权限漂移。

## 2. 权限模型

```text
enum RoleCode { ADMIN, PMO, GROUP_USER, EXPERT }

权限点（示例）：
  SYSTEM_USER_MANAGE, SYSTEM_GROUP_MANAGE, SYSTEM_DICT_MANAGE,
  AUDIT_VIEW, ANALYSIS_CONFIG, ANALYSIS_RUN,
  PROJECT_VIEW_ALL, PROJECT_VIEW_GROUP, PROJECT_EDIT, PROJECT_SUBMIT,
  PROJECT_REVIEW, PROJECT_APPROVE, PROJECT_TERMINATE,
  INSIGHT_TREND, INSIGHT_INVESTMENT, DECISION_PRECHECK, REPORT_EXPORT
```

- 角色 → 权限点映射集中在 `RolePermissionMapping`（代码常量），`@PreAuthorize("hasAuthority('...')")`。
- 数据范围：
  - `ADMIN/PMO`：全部。
  - `GROUP_USER`：`group_id = current`。
  - `EXPERT`：`project.status != DRAFT` 且（被指派 OR 处于 REVIEWING），只读。
- `DataScopeService` 提供 `applyProjectScope(wrapper)`、`assertProjectAccess(project)`，所有业务 Service 调用。
- 禁用用户：`JwtAuthenticationFilter` 每次请求校验 `status=ENABLED`，否则 401；角色/组变更后旧 token 的权限以数据库为准（过滤器重新加载用户）。

## 3. 系统管理接口（新增）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET/POST/PUT/DELETE | `/api/v1/groups` | SYSTEM_GROUP_MANAGE |
| GET/POST/PUT | `/api/v1/users` | SYSTEM_USER_MANAGE |
| POST | `/api/v1/users/{id}/reset-password` | SYSTEM_USER_MANAGE |
| PUT | `/api/v1/users/{id}/roles` | SYSTEM_USER_MANAGE |
| GET/POST/PUT/DELETE | `/api/v1/tech-directions` | SYSTEM_DICT_MANAGE |
| GET | `/api/v1/roles` | SYSTEM_USER_MANAGE |
| GET | `/api/v1/audit-logs` | AUDIT_VIEW |
| GET | `/api/v1/audit-logs/export` | AUDIT_VIEW |

- 删除校验：组下有用户/项目 → 400；技术方向被项目引用 → 400。
- 重置密码：生成临时密码返回一次，强制下次登录修改（`must_change_password` 字段，若 V1 未加则本任务补迁移 V6）。
- 审计查询：动态条件 + 分页；详情字段截断 2000 字。

## 4. 审批流

### 4.1 动作与状态

```text
SUBMIT        DRAFT/REJECTED -> SUBMITTED   GROUP_USER
WITHDRAW      SUBMITTED -> DRAFT            GROUP_USER(提交人)
ACCEPT        SUBMITTED -> REVIEWING        PMO
REJECT        SUBMITTED/REVIEWING -> REJECTED  PMO（意见必填）
APPROVE       REVIEWING -> APPROVED         PMO（要求 PASS 数达标）
START         APPROVED -> IN_PROGRESS       GROUP_USER
COMPLETE      IN_PROGRESS -> COMPLETED      GROUP_USER
TERMINATE     APPROVED/IN_PROGRESS -> TERMINATED  PMO
```

- 状态机全表在 `ProjectStateMachine`，首版已建骨架；本任务补齐动作权限、意见校验、PASS 数校验。
- 评审：`POST /projects/{id}/reviews`（专家），记录 round（当前最大 +1）、score、opinion、result；不可修改。
- 并发：`project.version` 乐观锁；冲突提示刷新。
- 审批轨迹写入 `project_approval_log`，详情页以时间线展示。

### 4.2 待办

```text
GROUP_USER: status in (DRAFT, REJECTED) 且本人/本组
PMO:        status in (SUBMITTED, REVIEWING)
EXPERT:     status = REVIEWING 且未提交本轮评审
```

- `GET /api/v1/tasks/todo` 汇总数量与列表；页面顶部徽标。

## 5. 前端

```text
views/system/{UserView,GroupView,TechDirectionView,RoleView,AuditLogView}.vue
views/review/ReviewTaskView.vue
components/project/ApprovalTimeline.vue
components/project/ReviewDialog.vue
components/layout/TodoBadge.vue
```

- 权限指令 `v-perm="'PROJECT_APPROVE'"` + 路由 `meta.permissions`；菜单按权限过滤。
- 用户管理：表格 + 抽屉表单 + 角色多选 + 重置密码确认。
- 审计：筛选栏 + 表格 + 详情抽屉 + 导出。
- 评审弹窗：评分、意见、结论；提交后刷新任务与轨迹。
- 审批时间线：from→to、动作、操作人、意见、时间。
- 角色演示账号切换后，页面元素与接口行为一致。

## 6. 测试

| 测试 | 内容 |
|------|------|
| PermissionMatrixTest | 4 角色 × 20+ 接口的允许/拒绝矩阵 |
| DataScopeAllTest | 项目/分析/趋势/投入/决策/导出的本组过滤 |
| UserDisabledTest | 禁用后旧 token 立即 401 |
| ApprovalFlowTest | 全状态合法/非法转移、意见必填、PASS 数校验 |
| ReviewTest | 多轮评审、不可修改、专家数据范围 |
| AuditQueryTest | 条件查询、导出、脱敏 |
| TodoTest | 各角色待办与状态一致 |

## 7. 风险

- 权限补丁式接入易漏：以矩阵测试为门禁，逐接口标注权限点。
- 旧 token 权限缓存：过滤器每次加载用户状态，避免禁用无效。
- 审批规则变化：PASS 数配置化，默认 1；变更写审计。
- 前端按钮与后端不一致：以后端 403 为准，前端仅做体验优化。

## 8. 实施口径（C1 实际落地）

- 权限：后端继续 `@PreAuthorize(hasAnyRole...)`，新增 `PermissionCatalog` 统一登记权限点与角色；`GET /roles` 返回角色与权限点；前端 `utils/permission.ts` 共用同一份目录。不新增权限表。
- 配置：V6 新增 `system_config(key,value,description,updated_by,updated_at)`，种子 `review.requiredPassCount=2`；管理页可改 1–10，非法值拒绝并写审计。
- PASS 口径：统计项目全部轮次中 `result=PASS` 的不同 `reviewer_id` 数量；同一专家多轮 PASS 只计 1 次；达到配置值才允许 `REVIEWING → APPROVED`。
- 专家范围：可见所有 `REVIEWING` 项目 + 同组非草稿项目；待办 = `REVIEWING` 且本人本轮未评审。
- 演示账号：demo profile 增加 expert02、expert03（生产不创建）。
- 审批门禁变化：APPROVE 从 `SUBMITTED → APPROVED` 调整为 `REVIEWING → APPROVED`；需同步修改 A2 既有测试与前端动作按钮。
- 系统管理接口统一 `/api/v1/system/*`，审计统一 `SYSTEM_*` 动作码；角色权限点目录同时用于前端菜单与矩阵测试。

# 角色权限、系统管理与审批流（收尾）— 实施计划

## 0. 开工前确认（已锁定）

- 单一任务，不拆分 C2。
- 专家评审：`REVIEWING` 全员可评，本人本轮未评即待办。
- 不做强制改密。
- PASS 数管理页可配，默认 2，按专家去重。
- 权限采用「角色注解 + 统一权限目录」，不新增权限表。
- demo profile 增加 expert02、expert03。

## 1. 顺序清单

- [x] 梳理权限点与角色映射；建立 `RolePermissionMapping` 与测试矩阵。
- [x] 在项目/分析/趋势/投入/决策/导出 Service 接入 `DataScopeService` 校验。
- [x] 用户管理 CRUD、启停、重置密码、角色分配；禁用即时失效。
- [x] 项目组与技术方向 CRUD、引用校验、启停、排序。
- [x] 审计查询与 CSV 导出、脱敏。
- [x] 补齐状态机动作权限、意见必填、PASS 数校验、乐观锁冲突提示。
- [x] 专家评审接口与多轮规则；评审记录不可修改。
- [x] 待办接口与页面徽标。
- [x] 前端系统管理页面（用户/组/字典/角色/审计）。
- [x] 前端评审任务页、评审弹窗、审批时间线。
- [x] 权限指令与路由 meta；菜单按权限过滤。
- [x] 测试：权限矩阵、数据范围全链路、禁用 token、审批全流程、审计、待办。
- [x] 用 4 个演示账号逐角色走查并截图。
- [x] V6：`system_config` 表与 `review.requiredPassCount=2` 种子；`ReviewConfigService`（1–10 校验）。
- [x] demo profile 增加 expert02、expert03 并更新演示账号说明。
- [x] `PermissionCatalog` + `GET /roles` 权限点 + 前端 `utils/permission.ts`。
- [x] 系统参数管理页与接口（读取/更新/审计）。
- [x] PASS 门禁按专家去重、配置热读取与测试。
- [x] 更新 A2 既有审批测试（ProjectWorkflowServiceTest、ProjectDeleteAndAuditTest）到新流程。

## 2. 验证命令

```powershell
mvn -q clean package
mvn test -Dtest='*Permission*,*DataScope*,*Approval*,*Review*,*Audit*,*Todo*'

java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

# 管理员维护用户
Invoke-RestMethod http://localhost:8080/api/v1/users -Headers $admin
# 专家评审
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/projects/1/reviews `
  -Headers $expert -ContentType 'application/json' `
  -Body '{"score":88,"opinion":"技术路线清晰","result":"PASS"}'
# 基金办立项
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/projects/1/actions `
  -Headers $pmo -ContentType 'application/json' -Body '{"action":"APPROVE","comment":"同意立项"}'
# 审计
Invoke-RestMethod "http://localhost:8080/api/v1/audit-logs?action=LOGIN" -Headers $admin

cd frontend
npm run build
npm run dev
```

- 逐角色验证：admin 全功能；pmo 无系统管理；user01 仅本组；expert01 仅评审与报告。
- 越权：user01 调用 `/users`、`/audit-logs`、`/projects/{他组}` 均 403。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| 权限注解 | 漏加导致越权 | 矩阵测试 + 逐接口清单 |
| `DataScopeService` | 过滤遗漏 | 全链路集成测试；默认拒绝策略 |
| 状态机 | 动作权限错配 | 全转移矩阵测试 |
| 用户禁用 | 旧 token 仍有效 | 过滤器校验状态 + 测试 |
| 审计导出 | 泄露敏感字段 | 统一脱敏器 + 用例 |

## 4. 完成定义（DoD）

- 权限矩阵测试全部通过；数据范围在六类业务场景均生效。
- 完整审批流可端到端演示，待办与轨迹一致。
- 系统管理页面可操作，删除/禁用/重置密码有明确反馈。
- 审计查询与导出可用，敏感字段不出现。
- 四角色走查截图齐全；`mvn test`、`npm run build` 通过。

## 5. 交接给交付

- 更新演示账号与角色说明、用户手册、截图。
- 交付清单中补充“权限矩阵验证记录”与四角色演示路径。

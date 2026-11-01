# 接口说明

基础路径：`/api/v1`；除登录/健康检查/静态资源外均需 `Authorization: Bearer <JWT>`。

## 认证

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/auth/login` | 匿名 | 登录，返回 token/roles/permissions |
| POST | `/auth/logout` | 登录 | 登出 |
| GET | `/auth/me` | 登录 | 当前用户 |
| PUT | `/auth/password` | 登录 | 修改密码 |
| GET | `/health` | 匿名 | 健康检查 |

## 项目与申报书

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET/POST | `/projects` | 登录 | 分页/创建（创建限 ADMIN、同组 GROUP_USER） |
| GET/PUT/DELETE | `/projects/{id}` | 登录 | 详情/编辑/删除 |
| PUT | `/projects/{id}/sections` | 登录 | 保存目标/内容/关键技术/节点/成员 |
| POST | `/projects/{id}/actions` | 登录 | SUBMIT/WITHDRAW/ACCEPT/APPROVE/REJECT/START/COMPLETE/TERMINATE |
| GET | `/projects/{id}/logs` | 登录 | 审批轨迹 |
| POST/GET | `/projects/{id}/reviews` | 登录 | 专家提交/查看评审 |
| POST | `/projects/{projectId}/documents` | 登录 | 上传附件 |
| POST | `/documents/{id}/parse` | 登录 | 解析并回填 |
| GET | `/documents/{id}/download` | 登录 | 下载 |
| DELETE | `/documents/{id}` | 登录 | 删除 |

## 工作台

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/dashboard/summary` | 登录 | 项目总数、已分析数、平均最高重复度、高风险数（按角色数据范围） |

## 分析

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET/PUT | `/analysis/config` | GET 登录 / PUT ADMIN | 当前/保存配置 |
| GET | `/analysis/config/history` | ADMIN | 配置历史 |
| POST | `/analysis/runs` | PMO/ADMIN | 触发分析 |
| GET | `/analysis/runs` | PMO/ADMIN | 运行列表 |
| GET/DELETE | `/analysis/runs/{id}` | 登录/ADMIN | 运行详情/删除 |
| GET | `/analysis/runs/{id}/pairs` | PMO/ADMIN | 运行报告：该次运行的项目对、分维度分、归因与重复/差异点 |
| GET | `/analysis/projects/{id}` | 登录 | 项目重复度报告 |
| GET | `/analysis/pairs/{id}` | 登录 | 相似对明细 |
| POST | `/analysis/precheck` | GROUP_USER/PMO/ADMIN | 预检（Top-1 可带模型解释） |
| POST | `/system/llm/test` | ADMIN | 大模型连接自检 |
| GET | `/analysis/llm-logs` | ADMIN/PMO | 模型调用日志分页、状态/模型/运行筛选与统计 |

## 趋势/投入/决策

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/insight/trend`、`/insight/trend/attribution`、`/insight/trend/heatmap`、`/insight/trend/drill` | GROUP_USER/PMO/ADMIN |
| GET | `/insight/trend/export` | GROUP_USER/PMO/ADMIN |
| GET | `/insight/investment/overview`、`/by-dimension`、`/risk`、`/export` | GROUP_USER/PMO/ADMIN |
| POST | `/decision/precheck` | GROUP_USER/PMO/ADMIN（EXPERT 403） |
| GET/POST | `/decision/records`、`/decision/records/{id}`、`/decision/records/{id}/void` | 见权限目录 |
| GET | `/decision/records/{id}/report.docx`、`report.xlsx` | 登录 + 数据范围 |
| GET/PUT | `/decision/rules` | ADMIN |

## 系统管理

| 方法 | 路径 | 权限 |
|------|------|------|
| GET/POST/PUT/DELETE | `/groups`、`/tech-directions` | ADMIN |
| GET/POST/PUT | `/users`、`/users/{id}/reset-password`、`/users/{id}/roles` | ADMIN |
| GET | `/roles` | ADMIN |
| GET/PUT | `/system/configs` | ADMIN |
| GET | `/audit-logs`、`/audit-logs/export` | ADMIN |
| GET | `/tasks/todo` | 四角色 |

## 错误码

`40000` 参数错误、`40012` 密码弱、`40101` 凭据错误、`40102` 锁定、`40103` Token 非法、`40105` 禁用、`40301` 越权、`40400` 不存在、`40500` 方法不支持、`50000` 内部错误。失败响应不返回堆栈、SQL、Key。

## 示例

```powershell
$body = @{username='admin'; password='Admin@123'} | ConvertTo-Json
$token = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType 'application/json' -Body $body).data.token
Invoke-RestMethod http://localhost:8080/api/v1/projects -Headers @{Authorization="Bearer $token"}
```

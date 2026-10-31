# 前端 API 集成与鉴权（A1）

## 1. Scope / Trigger

新增或修改接口调用、登录态、401/403 处理、路由守卫时适用。

## 2. Signatures

```text
http = axios.create({ baseURL: '/api/v1', timeout: 15000 })
loginApi(username, password): Promise<LoginResponse>
logoutApi(): Promise<void>
meApi(): Promise<UserInfo>
changePasswordApi(oldPassword, newPassword): Promise<void>
getDictsApi(): Promise<Dicts>
getGroupsApi(): Promise<GroupItem[]>
getTechDirectionsApi(): Promise<TechDirection[]>
```

## 3. Contracts

- 请求头：存在 token 时注入 `Authorization: Bearer <token>`。
- 响应包：`{code, message, data, timestamp}`；`code !== 0` 视为业务失败。
- 401：清除 `fund_platform_token`；若当前不在 `/login`，整页跳转 `/login?redirect=<path+search>`。
- 403：跳转 `/403`，已在 `/403` 时不重复跳转。
- 错误消息：优先使用后端 `message`；无响应体时使用中文兜底，不直接展示英文网络异常。
- 登录成功：保存 token/user/roles/permissions → 加载字典（失败忽略）→ 跳转 `redirect` 或 `/dashboard`。
- `redirect` 仅接受以单个 `/` 开头且不以 `//` 开头的站内路径。

## 4. Validation & Error Matrix

| 状态/条件 | 处理 |
|-----------|------|
| 400/422 | 页面展示后端 message 或字段错误 |
| 401 | 清 token，跳登录（登录页内不跳） |
| 403 | 跳 `/403` |
| 500 | 展示“服务器异常”，保留当前页面 |
| 网络错误/超时 | 展示中文兜底提示 |
| `code !== 0` | Promise reject，调用方展示 message |

## 5. Good / Base / Bad

- Good：token 过期访问受保护页 → 401 → 清 token → 登录页带 redirect。
- Base：登录成功但字典失败 → 仍进入工作台，字典区可重试。
- Bad：401 与 403 同时触发整页跳转，造成循环或丢失 redirect。
- Bad：登录页错误密码 401 被跳转逻辑再次刷新页面。

## 6. Tests Required

- Playwright：登录成功、错误密码提示、刷新保持登录、无效 token 清理、已登录访问 `/login`、403、404、登出。
- 控制台：正常流程 0 error；401 场景允许浏览器网络错误记录。
- 构建：`npm.cmd run typecheck`、`npm.cmd run build` 均通过。

## 7. Wrong vs Correct

### Wrong

```ts
if (error.response?.status === 401) {
  window.location.href = '/login'
}
```

### Correct

```ts
if (status === 401) {
  localStorage.removeItem(TOKEN_KEY)
  const current = `${location.pathname}${location.search}`
  if (!current.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`
  }
}
```

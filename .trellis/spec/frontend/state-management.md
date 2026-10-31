# 前端状态管理（A1 实测）

## Store 划分

### auth（`src/stores/auth.ts`）

- `token`：`localStorage["fund_platform_token"]` 为唯一持久化凭据。
- `user`：`UserInfo | null`，刷新后由 `/auth/me` 恢复。
- `roles`、`permissions`：登录响应或 `/auth/me` 返回；`permissions` 刷新后可能为空，路由级判断优先使用 `roles`。
- 方法：`login`、`fetchMe`、`logout`、`clearSession`、`changePassword`、`hasRole`、`hasPermission`。
- 登出：先调用 `/auth/logout`（失败不阻断），再清 token、用户与角色。

### dict（`src/stores/dict.ts`）

- `dicts`：`/dicts` 聚合结果；`loading`、`error`。
- `load(force)`：已有数据且非强制时直接返回；并发调用复用同一个 `inflight` Promise，避免重复请求。
- 字典加载失败不阻断登录与页面进入，可在页面内重试。

## 路由守卫

- `meta.public=true`：登录页、403、404 可匿名访问。
- 无 token 访问受保护路由 → `/login?redirect=<fullPath>`。
- 有 token 但无 user → `fetchMe()`；失败清理会话回登录；成功再加载字典（失败忽略）。
- 已登录访问 `/login` → 尝试恢复用户后跳 `/dashboard`；恢复失败停留登录页。
- `meta.roles`：声明式角色限制；守卫命中不匹配时跳 `/403`。

## 异步与清理

- 登录提交使用 `submitting` 入口守卫，防止回车/双击重复请求。
- 路由切换不保留上一账号的字典/用户数据；`clearSession` 清理本地状态。
- 组件卸载后不写入已失效状态；图表类组件（A3 起）需在卸载时 `dispose`。

## Wrong vs Correct

### Wrong

```ts
try {
  await auth.fetchMe()
  await dict.load()
} catch {
  await auth.logout() // 字典失败误踢用户
}
```

### Correct

```ts
try {
  await auth.fetchMe()
} catch {
  await auth.clearSession()
  return { name: 'login' }
}
try {
  await dict.load()
} catch {
  // 字典失败不阻断
}
```

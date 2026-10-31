# 认证、权限与审计契约（A1）

## 1. Scope / Trigger

修改登录、JWT、角色、数据范围、账号锁定、审计时必须遵守本契约；这些是跨层安全边界，不因前端隐藏而放宽后端校验。

## 2. Signatures

### 认证接口

| 方法 | 路径 | 权限 | 请求 | 响应 data |
|------|------|------|------|-----------|
| POST | `/api/v1/auth/login` | 匿名 | `{username, password}` | `{token, expiresAt, user, roles, permissions}` |
| POST | `/api/v1/auth/logout` | 登录 | 无 | `null` |
| GET | `/api/v1/auth/me` | 登录 | 无 | `UserInfoVO` |
| PUT | `/api/v1/auth/password` | 登录 | `{oldPassword, newPassword}` | `null` |
| GET | `/api/v1/health` | 匿名 | 无 | `{status, app, version, profile, db, time}` |

### 只读接口

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/api/v1/dicts` | 登录 |
| GET | `/api/v1/groups` | 登录 |
| GET | `/api/v1/tech-directions` | 登录 |

### 关键类

```text
AuthService.login(LoginRequest): LoginResponse
AuthService.currentUser(): UserInfoVO
AuthService.changePassword(ChangePasswordRequest): void
LoginAttemptService.recordFailure(User, LocalDateTime): void   // REQUIRES_NEW
LoginAttemptService.clearFailure(Long, LocalDateTime): void    // REQUIRED
JwtTokenProvider.createToken(LoginUser): GeneratedToken
JwtTokenProvider.parse(String): Claims
DataScopeService.assertGroupAccess(Long): void
AuditAspect.around(ProceedingJoinPoint, Audit): Object
```

## 3. Contracts

### LoginRequest

- `username`：必填，最长 50。
- `password`：必填，最长 64。
- 空值/超长 → 400 `BAD_REQUEST`。

### LoginResponse

- `token`：HS256 JWT，claim 包含 `sub=username`、`uid`、`roles`。
- `expiresAt`：当前时间 + 8 小时（demo/prod 可由配置覆盖）。
- `user`：`UserInfoVO`，不包含 `passwordHash/failCount/lockedUntil`。
- `roles`：角色代码列表；`permissions`：菜单权限列表。

### 环境变量

| 变量 | 必填 | 说明 |
|------|------|------|
| `JWT_SECRET` | prod 必填 | ≥32 字节；demo 缺省使用内置开发密钥并 WARN |
| `ADMIN_INIT_PASSWORD` | prod 首次启动 | 管理员 `INIT` 哈希初始化；缺失时启动失败 |
| `SPRING_PROFILES_ACTIVE` | 否 | 默认 `demo`；同时含 `prod` 时按生产处理 |

### 锁定规则

- 连续失败 5 次 → `locked_until = now + 10 分钟`。
- 锁定期内即使密码正确也返回 `AUTH_LOCKED`。
- 锁定期结束后重新计数（不延续旧失败次数）。
- 成功登录清零失败计数并记录 `last_login_at`。
- 失败计数写入必须独立事务提交，不能被登录异常回滚。

### 数据范围

- `ADMIN`、`PMO`：全量。
- `GROUP_USER`：仅 `currentGroupId`。
- `EXPERT`：全部 `REVIEWING` 项目 + 本组非草稿项目，只读；可提交评审，不可改项目或看趋势/投入/系统管理。
- 任何按 ID 读取/修改业务数据后必须再次校验归属，不能只依赖列表过滤。

### 项目动作权限（C1）

| 动作 | 允许角色 |
|------|----------|
| 创建/编辑项目 | ADMIN；GROUP_USER（同组） |
| SUBMIT / WITHDRAW | ADMIN；GROUP_USER（同组，WITHDRAW 仅提交人） |
| ACCEPT / APPROVE / REJECT / TERMINATE | ADMIN、PMO |
| START / COMPLETE | ADMIN；GROUP_USER（同组） |
| 提交评审 | ADMIN、EXPERT（仅 `REVIEWING`，每轮一次） |

- PMO 不继承项目维护类动作；不要用 `isUnrestricted()`（数据范围）代替动作权限。
- 立项 PASS 门禁：项目全部轮次中 `result=PASS` 的不同专家数，同一专家多轮 PASS 只计 1 人；阈值取 `review.requiredPassCount`（1–10，默认 2）。

### 审计

- 登录、登出、改密标注 `@Audit`；后续所有写接口必须标注。
- 记录字段：`userId/username/action/resourceType/detail/ip/result/costMs`。
- `detail` 中敏感字段替换为 `***`；最长 1900 字符。
- 审计失败仅 warn，不阻断主流程。

## 4. Validation & Error Matrix

| 条件 | HTTP | 错误码 |
|------|------|--------|
| 用户名/密码为空或超长 | 400 | `40000` |
| 账号不存在 | 401 | `40101`（与密码错误一致，不泄露存在性） |
| 密码错误 | 401 | `40101` |
| 连续失败达到 5 次 | 401 | `40102` |
| 账号禁用 | 401 | `40105` |
| JWT 缺失 | 401 | `40103` / `40100` |
| JWT 非法或过期 | 401 | `40103` |
| 角色不足 | 403 | `40301` |
| 跨组访问 | 403 | `40301` |
| 旧密码错误 | 400 | `40104` |
| 新密码弱 | 400 | `40012` |
| 只读接口收到 POST/PUT | 405 | `40500` |

## 5. Good / Base / Bad

- Good：登录失败 5 次后第 6 次使用正确密码仍返回锁定；审计记录密码为 `***`。
- Base：管理员登录成功，`/me` 无敏感字段。
- Bad：`@Transactional` 内抛异常导致失败计数回滚，锁定永不生效。
- Bad：JWT 过滤器只解析 token 不校验用户状态，禁用用户仍可访问。
- Bad：403 被 `Exception.class` 兜底成 500。

## 6. Tests Required

- UT-01~UT-20（见 `test/用例设计.md`），至少断言：
  - 登录成功 token 非空、角色包含 ADMIN、过期时间在未来。
  - 5 次失败后第 6 次 `40102`，`locked_until` 已写入且晚于当前。
  - 禁用用户不能登录。
  - 非法/过期 JWT → 401 `40103`。
  - 匿名访问 `/dicts` → 401；`GROUP_USER` 访问 ADMIN 接口 → 403。
  - `/me` 不含密码哈希/失败计数/锁定时间。
  - `audit_log` 有 LOGIN/LOGOUT，detail 不含明文密码。
  - 锁定测试使用专用账号，避免污染演示账号。

## 7. Wrong vs Correct

### Wrong

```java
@Transactional
public LoginResponse login(LoginRequest request) {
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        userMapper.update(... failCount + 1 ...);
        throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS); // 事务回滚，计数丢失
    }
    ...
}
```

### Correct

```java
@Transactional
public LoginResponse login(LoginRequest request) {
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        loginAttemptService.recordFailure(user, now); // REQUIRES_NEW 独立提交
        throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
    }
    loginAttemptService.clearFailure(user.getId(), now);
    ...
}
```

### Wrong

```java
if (token != null) {
    // 只解析签名，不检查用户是否禁用
    SecurityContextHolder.getContext().setAuthentication(...);
}
```

### Correct

```java
LoginUser user = userDetailsService.loadUserByUsername(username);
if (!user.isEnabled()) {
    throw new BizException(ErrorCode.AUTH_DISABLED);
}
```

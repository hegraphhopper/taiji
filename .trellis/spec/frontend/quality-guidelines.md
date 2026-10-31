# 前端质量规范（A1 实测）

## 验证命令

```powershell
cd frontend
npm.cmd run typecheck     # vue-tsc --noEmit，strict
npm.cmd run build         # typecheck + vite build
npm.cmd run dev           # 仅开发时使用；不要在前台工具调用中启动
```

- Windows PowerShell 默认执行策略可能阻止 `npm.ps1`，统一使用 `npm.cmd`。
- Vite dev 端口 5173，代理 `/api` 到 8080。
- 当前主包约 1.1 MB（gzip 374 KB），Element Plus 全量引入；A1 可接受，后续可按需引入或 manualChunks。

## 运行态验收

- 使用 `scripts/start-demo.ps1` 启动（隐藏窗口），`scripts/status-demo.ps1` 检查，`scripts/stop-demo.ps1` 停止。
- 浏览器尺寸至少验证 1440×900 与 900×820；检查无横向溢出、侧栏可折叠。
- 正常流程控制台 0 error；401 场景允许浏览器记录网络 401。
- 截图用于验收，不把 Token、路径、账号密码写入截图。

## 交互与可访问性

- 表单必须有 label；错误消息可读且定位到字段。
- 键盘可提交（Enter），按钮可聚焦，焦点可见。
- 加载、空态、错误态、成功态分别展示。
- 用户内容使用插值渲染，禁止 `v-html`。
- 图标按钮必须有 `aria-label` 或 `title`。

## 约定

- 设计 token 只用 `styles/tokens.css`；不新增全局样式覆盖组件库。
- 页面按 feature 目录组织；API 调用集中在 `api/`。
- 不硬编码生产密钥；演示账号提示仅用于 demo。
- 修改后运行 typecheck 与 build，并记录实际结果。

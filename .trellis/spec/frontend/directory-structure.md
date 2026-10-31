# 前端目录结构（A1 实测）

## 技术栈

Vue 3.5 + TypeScript 5.6 + Vite 5.4 + Element Plus 2.8 + Pinia 2.2 + Vue Router 4.4 + Axios 1.7。

## 目录

```text
frontend/
  index.html
  package.json / package-lock.json
  vite.config.ts                 代理 /api -> http://localhost:8080
  tsconfig.json / tsconfig.node.json
  public/favicon.svg
  src/
    main.ts                      createApp、Pinia、Router、Element Plus 中文
    App.vue
    env.d.ts                     *.vue 类型声明
    api/
      http.ts                    Axios 实例、Bearer、401/403、统一错误
      auth.ts                    login/logout/me/changePassword
      system.ts                 dicts/groups/tech-directions
      project.ts                项目 CRUD、状态动作、附件上传/解析/下载
      analysis.ts               分析配置、运行触发/历史/删除、项目报告、项目对、预检
    stores/
      auth.ts                    token/user/roles/permissions、登录/登出/改密
      dict.ts                    字典缓存、在途请求去重
    router/index.ts              路由、守卫、meta.public/title/roles
    layouts/MainLayout.vue       侧栏、顶栏、改密弹窗、登出
    views/
      login/LoginView.vue
      dashboard/DashboardView.vue
      project/ProjectListView.vue
      project/ProjectFormView.vue
      project/ProjectDetailView.vue
      analysis/AnalysisConfigView.vue   分析配置（ADMIN）
      analysis/AnalysisRunsView.vue     分析运行（PMO/ADMIN）
      error/ForbiddenView.vue
      error/NotFoundView.vue
    styles/tokens.css            设计 token
    styles/index.css             全局样式
    types/index.ts               ApiResult/UserInfo/Dicts 等
    utils/redirect.ts            站内跳转校验
    utils/project.ts             状态标签、金额与日期格式化
```

## 归属约定

- 页面放 `views/<feature>/`，可复用组件放 `components/<feature>/`（A2 起引入）。
- API 封装统一放 `api/`，页面不直接使用 Axios。
- 跨页面共享状态放 `stores/`；局部表单状态保留在页面内。
- 设计 token 只放在 `styles/tokens.css`；页面样式使用 `scoped`。
- 新增依赖前确认是否已有 Element Plus / Vue 能力可覆盖。

## 构建产物

- `frontend/dist/` 与 `frontend/node_modules/` 已被 `.gitignore` 忽略。
- 生产集成方式见 README；A1 以 Vite dev + 后端 API 联调为准。

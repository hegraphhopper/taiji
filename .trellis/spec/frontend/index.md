# 前端开发规范

Vue 3 + TypeScript + Vite 5 + Element Plus 2.8 + Pinia + Vue Router，中文界面，数据密集型后台风格。

## 规范索引

| 文件 | 用途 |
|------|------|
| [目录结构](./directory-structure.md) | 实际目录与文件归属 |
| [组件规范](./component-guidelines.md) | 组件、样式及新界面设计 |
| [Hook 规范](./hook-guidelines.md) | 副作用、数据获取与清理 |
| [状态管理](./state-management.md) | auth/dict store、守卫与异步 |
| [类型安全](./type-safety.md) | 接口 DTO 与空值处理 |
| [API 集成](./api-integration.md) | Axios、401/403、redirect 契约 |
| [质量规范](./quality-guidelines.md) | 构建、验收、可访问性 |

## 开发前检查（Pre-Development Checklist）

- 读取任务 `prd.md`、`design.md`、`implement.md` 与用例。
- 确认现有组件、store、api 封装与设计 token，优先复用。
- 新界面按需调用 `frontend-design`；已有界面改进按需调用 `redesign-existing-projects`。
- 从 `frontend/package.json` 确认脚本，使用 `npm.cmd`。

## 质量检查（Quality Check）

- 运行 `npm.cmd run typecheck` 与 `npm.cmd run build`。
- 用 Playwright 验证登录、刷新、401、403、404、登出、900/1440 布局。
- 正常流程控制台无错误；记录截图与命令结果。
- 规范同步内容使用中文；代码与命令保持原样。

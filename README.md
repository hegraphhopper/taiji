# 自投研发项目重复投入治理平台

面向电科太极平台基金、创新基金等自投研发项目的项目管理与重复投入治理平台。A 阶段聚焦核心主线：**结构化申报书 → 多维重复度分析 → 重复度趋势与归因**。

当前版本为 A1 基础版：完成工程底座、最小登录鉴权、组织/技术方向种子、审计写入、前端应用壳。项目管理、分析引擎、趋势归因分别在 A2–A4 实现；角色管理与 OA 审批流在 C 阶段收尾。

## 技术栈

| 层 | 选型 |
|----|------|
| 后端 | Spring Boot 2.7.18、Spring Security 5.7、JWT（jjwt 0.11.5）、MyBatis-Plus 3.5.3.2、Flyway |
| 数据库 | 演示 H2 2.1（文件模式，零依赖）；生产 MySQL 8 |
| 前端 | Vue 3 + TypeScript + Vite 5、Element Plus、Pinia、Vue Router、Axios |
| 构建 | JDK 8、Maven 3.5+、Node 18+ |

## 环境要求

- JDK 1.8
- Maven 3.5 及以上
- Node.js 18 及以上（当前开发环境为 Node 24）
- 可选：MySQL 8（生产部署）；本地演示无需安装数据库

## 快速启动（推荐）

在项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-demo.ps1
```

脚本会：

1. 若 `target/fund-platform-1.0.0.jar` 不存在则执行 `mvn -DskipTests package`；
2. 以隐藏窗口启动后端（demo profile），等待健康检查通过；
3. 若 `frontend/node_modules` 不存在则执行 `npm install`；
4. 以隐藏窗口启动 Vite 开发服务，等待页面可访问；
5. 输出访问地址与演示账号。

已构建过、只想快速启动：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-demo.ps1 -SkipBuild
```

查看状态：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/status-demo.ps1
```

停止：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-demo.ps1
```

> Windows PowerShell 默认执行策略可能阻止 `.ps1`，因此命令中带 `-ExecutionPolicy Bypass`。脚本使用隐藏窗口启动，不会占用当前终端。

## 访问地址与演示账号

| 入口 | 地址 |
|------|------|
| 前端 | http://127.0.0.1:5173 |
| 后端健康检查 | http://localhost:8080/api/v1/health |
| OpenAPI / Swagger | http://localhost:8080/swagger-ui.html |

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `Admin@123` | 系统管理员 |
| `pmo01` | `Pmo@12345` | 基金办 |
| `user01` | `User@12345` | 项目组用户（智能计算项目组） |
| `expert01` | `Expert@12345` | 评审专家 |

首次登录后建议修改密码。演示账号仅用于本地与评审演示，不要用于生产。

## 手动启动

后端：

```powershell
mvn -q clean package
java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo
```

前端（另开终端）：

```powershell
cd frontend
npm install
npm run dev
```

生产构建：

```powershell
cd frontend
npm run build
```

## 配置说明

公共配置见 `src/main/resources/application.yml`，演示配置见 `application-demo.yml`，生产配置见 `application-prod.yml`。

| 环境变量 | 说明 | 默认 |
|----------|------|------|
| `SERVER_PORT` | 后端端口 | `8080` |
| `SPRING_PROFILES_ACTIVE` | 运行 profile | `demo` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接（prod） | 无 |
| `JWT_SECRET` | JWT 签名密钥，prod 必填 | demo 有开发密钥 |
| `UPLOAD_DIR` | 附件存储目录 | `./storage` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端来源 | `http://localhost:5173,http://127.0.0.1:5173` |
| `ADMIN_INIT_PASSWORD` | prod 首次启动管理员密码 | 无 |

演示数据落在 `./data/funddb.*`（H2 文件库），首次启动由 Flyway 自动建表并写入种子。删除 `data/` 可重建演示库。

## 目录结构

```text
.
├─ src/main/java/com/taiji/fund/     后端源码
│  ├─ common/                        统一响应、异常、枚举、审计注解
│  ├─ config/                        Security、MyBatis-Plus、Jackson、OpenAPI、CORS
│  ├─ security/                      JWT、过滤器、登录服务、数据范围钩子
│  ├─ system/                        用户/角色/项目组/技术方向/字典/审计
│  ├─ health/                        健康检查
│  └─ bootstrap/                     演示账号初始化
├─ src/main/resources/
│  ├─ application*.yml               配置
│  └─ db/migration/                  Flyway V1 建表、V2 种子
├─ frontend/                         Vue 3 + TypeScript 前端
├─ scripts/                          start/stop/status 脚本
├─ docs/                             产品设计与交付文档
├─ data/                             本地 H2 数据（git 忽略）
├─ storage/                          附件存储（git 忽略）
└─ target/                           构建产物与运行日志（git 忽略）
```

## A1 已实现能力

- 登录 / 登出 / 当前用户 / 修改密码
- JWT 无状态鉴权，未认证 401、无权限 403 统一 JSON
- 登录失败 5 次锁定 10 分钟；禁用用户 token 立即失效
- 组织、技术方向种子与只读接口；字典聚合接口
- 审计写入（登录、登出、改密），密码字段脱敏
- 前端登录页、主布局、路由守卫、工作台占位、403/404
- 演示账号初始化、健康检查、Swagger
- 启动 / 停止 / 状态脚本

## A1 暂未包含

- 项目与申报书管理（A2）
- 重复度分析与趋势归因（A3/A4）
- 用户/角色/项目组/技术方向管理界面（C）
- 形式审查、专家评审、完整审批流（C）
- 投入态势、辅助决策与报告导出（B）

## 相关文档

- `docs/A阶段产品设计书.md`：A 阶段产品、交互、数据、接口、算法与验收基线
- `.trellis/tasks/10-29-fund-foundation/prd.md`：A1 需求与验收标准
- `.trellis/tasks/10-29-fund-dup-platform/`：整体规划、设计、子任务拆分

## 常见问题

**端口被占用**：先执行 `scripts/stop-demo.ps1`，或修改 `SERVER_PORT` 与 Vite 端口。

**登录后立即回到登录页**：检查后端是否健康、`JWT_SECRET` 是否在重启后变化；浏览器本地 token 可通过退出登录清除。

**H2 数据库被锁**：确认没有第二个后端进程占用 `data/`；执行停止脚本后重启。

**前端依赖安装慢**：可使用国内镜像，例如 `npm config set registry https://registry.npmmirror.com`，再重新 `npm install`。

**日志位置**：`target/app.out.log`、`target/app.err.log`、`target/vite.out.log`、`target/vite.err.log`。

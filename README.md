# 自投研发项目重复投入治理平台

面向电科太极平台基金、创新基金等自投研发项目的项目管理与重复投入治理平台。A 阶段聚焦核心主线：**结构化申报书 → 多维重复度分析 → 重复度趋势与归因**。

当前版本为交付版：核心主线（结构化申报书、重复度分析、趋势归因）、增强功能（投入态势、辅助决策、报告导出）、治理收尾（四角色权限、审批流、审计）与交付物（演示数据、备份、文档、截图、演示视频）均已完成；大模型为可选解释层，默认 100/0 不改变总分。

- 代码仓库：`https://github.com/hegraphhopper/taiji`
- 交付分支：`wecode/member-shangcirenwuzhix-779d`
- 本地演示地址：`http://127.0.0.1:5173`（WeCode 地址待环境提供后补）

## 技术栈

| 层 | 选型 |
|----|------|
| 后端 | Spring Boot 2.7.18、Spring Security 5.7、JWT（jjwt 0.11.5）、MyBatis-Plus 3.5.3.2、Flyway |
| 数据库 | 演示 H2 2.1（文件模式，零依赖）；生产 MySQL 8 |
| 前端 | Vue 3 + TypeScript + Vite 5、Element Plus、Pinia、Vue Router、Axios |
| 构建 | JDK 8、Maven 3.5+、Node 18+ |

## 环境依赖

| 依赖 | 版本要求 | 用途 | 是否必需 |
|------|----------|------|----------|
| JDK | 1.8（已适配 1.8.0_65） | 后端编译与运行 | 必需 |
| Maven | 3.5 及以上 | 后端构建 | 必需 |
| Node.js / npm | Node 18 及以上（开发环境 Node 24） | 前端构建与开发服务 | 必需 |
| H2 | 随依赖内置 2.1.214 | 本地演示数据库（文件模式） | 自动 |
| MySQL | 8.0 | 生产数据库 | 可选 |
| Docker / Docker Compose | 20.10+ / v2 | 容器化部署 | 可选 |

- 默认端口：后端 `8080`，前端 `5173`；被占用时先停止旧进程或修改 `SERVER_PORT`。
- 本地演示不需要安装 MySQL 或 Docker；首次启动会自动建表并写入演示数据。
- Windows PowerShell 执行脚本需带 `-ExecutionPolicy Bypass`；手动命令在 Linux/macOS 下等效。

### 首次启动步骤

```powershell
# 1. 克隆代码
git clone https://github.com/hegraphhopper/taiji.git
cd taiji
git checkout wecode/member-shangcirenwuzhix-779d

# 2. 确认依赖版本
java -version          # 1.8.x
mvn -version           # 3.5+
node -v && npm -v      # Node 18+

# 3. 一键启动（首次会构建后端并安装前端依赖）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-demo.ps1
```

启动完成后访问 `http://127.0.0.1:5173`，使用下方演示账号登录。

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

## 构建与测试

```powershell
# 后端全量测试与打包
mvn -q test
mvn -q -DskipTests package

# 前端类型检查与生产构建
cd frontend
npm.cmd run typecheck
npm.cmd run build
```

- 测试使用 H2 内存库，不依赖本机 MySQL。
- 性能用例默认排除；需要时执行 `mvn test "-Dtest=AnalysisPerformanceTest" "-Dtest.excludedGroups=none"`。
- 构建产物：`target/fund-platform-1.0.0.jar`、`frontend/dist/`（均不入库）。

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
| `LLM_ENABLED` | 是否启用大模型语义复核 | `false` |
| `LLM_BASE_URL` / `LLM_MODEL` / `LLM_API_KEY` | OpenAI 兼容接口地址、模型、密钥（仅环境变量，不落库不写日志） | 无 |
| `DB_SEED_ENABLED` | 首次启动用内置 H2 快照初始化数据文件（部署用） | `false` |
| `DB_SEED_TARGET` / `DB_SEED_MARKER` | 快照目标数据文件与初始化标记文件 | 无 |
| `DB_SEED_RESOURCE` | 内置快照 classpath 路径 | `classpath:db/seed/funddb.mv.db` |

演示数据落在 `./data/funddb.*`（H2 文件库），首次启动由 Flyway 自动建表并写入种子。删除 `data/` 可重建演示库。

## 目录结构

```text
.
├─ src/main/java/com/taiji/fund/     后端源码
│  ├─ common/                        统一响应、异常、枚举、审计注解
│  ├─ config/                        Security、MyBatis-Plus、Jackson、OpenAPI、CORS
│  ├─ security/                      JWT、过滤器、登录服务、数据范围
│  ├─ system/                        用户/角色/项目组/技术方向/字典/审计
│  ├─ project/                       项目主数据、结构化内容、状态机、查询
│  ├─ document/                      附件上传、文本提取、章节解析
│  ├─ analysis/                      分析配置、分词/向量/打分、LLM、运行与项目对
│  ├─ insight/trend/                 趋势聚合、归因矩阵、热力图、下钻与 CSV
│  ├─ insight/investment/            投入态势、风险敞口、多维分布与导出
│  ├─ decision/                      预检、建议规则、决策记录、Word/Excel 报告
│  ├─ dashboard/                     工作台汇总指标
│  ├─ health/                        健康检查
│  └─ bootstrap/                     演示账号与演示数据初始化
├─ src/main/resources/
│  ├─ application*.yml               公共/演示/生产配置（含 app.llm.*，默认关闭）
│  ├─ terms/tech-terms.txt           内置技术术语词典（310 条）
│  ├─ prompts/duplicate-analysis.txt LLM 查重提示词
│  ├─ db/migration/                  Flyway V1–V8（系统、项目、分析、决策、大模型）
│  └─ db/seed/funddb.mv.db           可选：部署用 H2 演示数据快照
├─ frontend/                         Vue 3 + TypeScript 前端
│  ├─ src/api/                       按模块封装的接口
│  ├─ src/views/                     工作台、项目、分析、趋势、投入、决策、系统
│  ├─ src/components/                图表、报告弹窗与通用组件
│  └─ src/stores/                    登录态、字典、应用状态
├─ scripts/                          启动/停止/状态、趋势回填、备份/恢复脚本
├─ db/                               H2 备份、MySQL schema/data、部署说明
├─ docs/                             需求、设计、接口、用户手册、指标、演示与截图
├─ docs/review/                      需求分析/系统设计 DOCX 与演示视频材料
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
- 前端登录页、主布局、路由守卫、工作台真实指标（项目总数/平均最高重复度/高风险项目）、403/404
- 演示账号初始化、健康检查、Swagger
- 启动 / 停止 / 状态脚本

## A2 已实现能力

- 项目主数据：自动编码 `TJJH-{年}-{4 位}`、基金类型、项目组、负责人、技术方向、预算、起止时间
- 结构化申报内容：项目目标、研制内容、关键技术（难度）、节点计划（权重）、项目成员（角色/人月）
- 附件：docx/pdf/txt/md 上传（≤20MB、每项目 ≤10 个）、解析正文、章节预览与一键回填、失败原因可见
- 状态机：`DRAFT → SUBMITTED → APPROVED / REJECTED`，提交完整性校验、撤回、驳回意见必填、非法流转拒绝
- 审批轨迹与审计：项目增删改、内容保存、状态动作、附件上传/解析/删除全部留痕
- 数据范围：ADMIN/PMO 全量、GROUP_USER 本组、EXPERT 已提交项目只读；后端强制过滤
- 前端：项目列表（筛选/分页/排序）、分区新建编辑表单、详情六 Tab、解析预览与回填

## A3 已实现能力

- 分析配置：五维字段权重、等级阈值、Top-K、同期窗口、大模型开关/权重/候选数/超时；版本自增、历史可查、仅 ADMIN 可改
- 文本处理：全角/HTML/控制符清洗、中文 2-gram + 英文词 + 术语词典加权（×3）、运行级 TF-IDF + 余弦
- 五维打分：目标 / 研制内容 / 关键技术 / 节点计划 / 技术标签，按配置权重归一化；等级 HIGH/MEDIUM/LOW/NONE
- 归因：本组内部 / 他组在先 / 我方在先 / 同期并行，存中性事实、按查看视角翻译
- 可解释：每对输出分维度得分、重复片段与差异片段
- 可选大模型：OpenAI 兼容接口，仅复核 Top-N，超时/500/非法 JSON 自动降级，单次运行连续 3 次失败熔断，24h 缓存、调用日志脱敏
- 运行：全量 / 单项目 / 预检，并发保护（重复触发 409），失败清理，`analysisDate` 历史回填
- 快照与报告：项目级最高分、Top-K 平均、高风险对数、四类归因计数、风险敞口；项目详情「重复度」Tab 展示
- 运行报告：分析运行历史「查看报告」直接展示该次运行的项目对、总分/等级/中性归因、五维分与重复/差异点；支持只看高风险与分页
- 前端：分析运行页、分析运行报告、分析配置页（ADMIN）、菜单按角色显隐
- 性能：300 项目全量分析约 1.5s（门禁 5s）；性能用例默认排除，手动执行 `mvn test "-Dtest=AnalysisPerformanceTest" "-Dtest.excludedGroups=none"`

## A4 已实现能力

- 趋势指标：按月/季/年聚合，分析项目数、平均最高分、平均 Top-K 分、高风险项目数、项目对战与唯一对数、高风险对数、环比
- 同比口径：同一项目在同一时间粒度内只取最新一次运行，跨月/跨季保留历史
- 归因：四类计数与占比（项目视角）、按项目组归因矩阵；页面与 CSV 均标注"跨组对双方各计一次"
- 热力图：项目组 × 月份、技术方向 × 项目组，可切换平均分/高风险项目数，tooltip 显示样本数
- 下钻：点击趋势点 → 项目清单（可按等级过滤）→ 进入项目详情查看重复度报告；下钻条件写入 URL
- 结论摘要：规则生成 3–5 句，数字与图表同源
- 导出：UTF-8 BOM CSV，含口径说明与生成时间
- 权限：PMO/ADMIN 全量，GROUP_USER 强制本组，EXPERT 无权访问（菜单隐藏 + 403）
- 演示数据：`demo` 启动自动生成 **60 个项目**（2024–2026、8 个项目组、7 种状态、四类归因样本；`DEMO_DATA_SEED=false` 可关闭），配合 `scripts/backfill-trend.ps1 -Months 14` 生成近 14 个月真实快照
- 性能：60 项目 × 24 月聚合约 133ms（门禁 500ms）；性能用例默认排除，手动执行 `mvn test "-Dtest=TrendPerformanceTest" "-Dtest.excludedGroups=none"`

### 演示数据初始化

```powershell
# 1) 启动演示（首次自动写入 60 个演示项目，幂等）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-demo.ps1
# 2) 以 ADMIN 身份逐月回填近 14 个月分析快照（约 1-3 分钟）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/backfill-trend.ps1 -Months 14
# 3) 打开 http://127.0.0.1:5173/trend
```

演示数据规模：60 个项目、8 个项目组、4 类技术方向分布、7 种状态；同组重复 2 对、跨组在先 6 对、同期并行 2 对，其余为低重复/无关主题；每项目含目标、研制内容、关键技术、节点与成员。

## B 阶段已实现能力

- 投入态势：投入总额、项目数、在研/结项、平均预算、平均重复度、高风险项目数、重复投入风险敞口。
- 多维分析：按年度、基金类型、项目组、技术方向；同比/环比、Top 风险、组间对比、散点与下钻。
- 辅助决策：新项目预检输出风险分、等级、Top-5 相似项目、重复/差异点、建议动作、预计重复投入金额与建议专家。
- 决策记录：保存预检快照与操作人，可追溯；支持 Word 决策报告与 Excel 明细导出。

## C 阶段已实现能力

- 四类角色（ADMIN/PMO/GROUP_USER/EXPERT）菜单与接口权限；服务层强制数据范围。
- 用户、角色、项目组、技术方向、系统参数与审计日志管理。
- 形式审查、专家评审（评分/意见/结论）、立项/驳回、在研/结项完整状态机与审批轨迹。
- 待办中心与评审任务；关键写操作全部留痕。

## D 阶段交付物

- 60 个项目、8 个项目组、2024–2026 三个年度、7 种状态的演示数据；14 个月趋势快照。
- H2 文件库备份/恢复脚本，MySQL 8 schema 与部署说明，Docker 配置，演示视频与截图。
- 需求分析、系统设计、数据库、接口、用户手册、指标口径、演示脚本与提交清单。

## 已包含

- A 阶段：登录/JWT/角色、项目与申报书、附件解析、重复度分析、运行报告、趋势归因。
- B 阶段：投入态势、辅助决策、决策记录、Word/Excel 报告。
- C 阶段：四角色权限、用户/组/字典/参数/审计、专家评审、审批流、待办。
- D 阶段：60 项目演示数据、14 个月趋势回填、H2 备份脚本、MySQL schema、文档与截图。
- 大模型：连接自检、解释层（不改变总分）、预检 Top-1 复核、报告模型复核段。

## 部署与数据

- 本地演示：`scripts/start-demo.ps1`。
- H2 备份：`scripts/backup-db.ps1`；恢复：`scripts/restore-db.ps1 -Archive db/backup/<file>.zip -Force`。
- 部署内置数据：设置 `DB_SEED_ENABLED=true`、`DB_SEED_TARGET=/data/funddb.mv.db`、`DB_SEED_MARKER=/data/.db-seeded`，首次启动会把 `db/seed/funddb.mv.db` 复制到目标位置；标记文件存在后不会覆盖运行期数据。
- MySQL：`db/mysql/schema.sql`（V1–V8）、`db/mysql/data.sql`，详见 `db/mysql/README.md`。
- Docker：`Dockerfile` + `docker-compose.yml`（MySQL 8 + 应用）；本机无 Docker，未实际运行验证。
- WeCode：入口/环境未提供，部署地址待补；本地地址 `http://127.0.0.1:5173`。
- 大模型：设置 `LLM_ENABLED=true`、`LLM_BASE_URL`、`LLM_MODEL`、`LLM_API_KEY`（可写入根目录 `application-local.yml`，启动脚本会自动加载）；管理页“分析配置”可测试连接。默认 `localWeight=100 / llmWeight=0`，模型分只作解释、不改变总分；超时/连接异常/5xx 最多重试 1 次，4xx 与非法 JSON 不重试，连续 3 次调用失败后熔断。
- 大模型限制：候选对串行调用，无并发池；结果为进程内 24h 缓存，重启或多实例不共享；仅支持 OpenAI 兼容单一端点。
- Windows + JDK 8 若出现 `PKIX path building failed`，启动脚本已加入 `-Djavax.net.ssl.trustStoreType=Windows-ROOT`；Linux 需将目标站点 CA 导入 JDK truststore。
- 模型调用日志：分析中心 → 模型调用日志（ADMIN/PMO），查看状态、耗时与 Token，不记录 Prompt/Key。

## 相关文档

- `docs/requirements-analysis.md`：需求、角色、流程、验收
- `docs/design.md`：架构、模块、算法、部署
- `docs/database.md`：表、索引、口径
- `docs/api.md`：接口、权限、错误码、示例
- `docs/user-guide.md`：四角色操作手册
- `docs/metrics.md`：重复度、归因、风险敞口、PASS 门禁
- `docs/demo-data.md`：演示数据规模与账号
- `docs/demo-script.md`：5 分钟演示脚本
- `docs/submission-checklist.md`：四项交付物清单
- `docs/A阶段产品设计书.md`：A 阶段产品与交互基线
- `docs/review/需求分析.docx`、`docs/review/系统设计.docx`：评审版文档
- `docs/review/video/`：演示视频、字幕、分镜与录制指南
- `.trellis/spec/backend/analysis-guidelines.md`：分析口径、接口、归因与降级契约
- `.trellis/spec/backend/insight-guidelines.md`：趋势去重、项目视角口径、下钻与 CSV 契约
- `.trellis/tasks/10-29-fund-dup-platform/`：整体规划、设计、子任务拆分

## 常见问题

**端口被占用**：先执行 `scripts/stop-demo.ps1`，或修改 `SERVER_PORT` 与 Vite 端口。

**登录后立即回到登录页**：检查后端是否健康、`JWT_SECRET` 是否在重启后变化；浏览器本地 token 可通过退出登录清除。

**H2 数据库被锁**：确认没有第二个后端进程占用 `data/`；执行停止脚本后重启。

**前端依赖安装慢**：可使用国内镜像，例如 `npm config set registry https://registry.npmmirror.com`，再重新 `npm install`。

**日志位置**：`target/app.out.log`、`target/app.err.log`、`target/vite.out.log`、`target/vite.err.log`。

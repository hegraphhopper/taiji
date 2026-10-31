# 交付物与部署验证 — 技术设计

## 1. 演示数据设计

### 1.1 初始化方式

```text
@Profile("demo") DemoDataInitializer implements ApplicationRunner
  if (!app.demo.enabled) return
  if (repo.count(project) > 0) { log skip; return; }   // 幂等

  1) 组织/用户/角色（foundation 已种子，若缺失则补）
  2) 60 个项目：固定 seed=20261029 的 Random，模板化生成
  3) 子表：目标 2–3 条、研制内容 3–5 条、关键技术 2–4 条、
          节点 3–5 条、成员 2–4 人、附件元数据
  4) 状态与时间：按年度分布，submit_time 落在 2024-07 ~ 2026-09
```

- 数据仅写库，不依赖网络；附件文件在 `demo/attachments/`，初始化时复制到 `storage/`。
- 通过 `app.demo.enabled`（默认 demo profile 为 true）控制；生产 profile 禁止。

### 1.2 内容模板与重复模式

```text
主题池：多模态质检、知识图谱、低代码平台、边缘智能、密码应用、
        工业协议、数字孪生、可靠性预测、数据治理、隐私计算…

模式分配（项目对）：
  SELF_GROUP   : (G1,P1)-(G1,P2) 内容重合 85–95%
  PEER_EARLIER : (G2,P3 早 6 个月)-(G4,P4) 重合 80–90%
  OUR_EARLIER  : (G5,P5 早 4 个月)-(G6,P6) 重合 70–85%
  SIMULTANEOUS : (G3,P7 与 G7,P8 相差 10 天) 重合 65–80%
  LOW          : 其余项目 15–40%
```

- 用模板句子拼装，确保字符 n-gram/TF-IDF 能产生稳定分数；同义改写少量，用于演示大模型可选项。
- 每个项目预算、状态、技术方向随机但受约束（同组项目方向相近）。
- 项目编码 `TJJH-{year}-{seq}`。

### 1.3 历史回填

```text
for month in last 14 months:
  analysisDate = month.end
  corpus = projects where submit_time <= analysisDate
  run = AnalysisExecutor.run(trigger=FULL, analysisDate=analysisDate, operator=admin)
  run.skipLlm = true   // 回填不调用外部
```

- 回填在 `DemoBackfillRunner`（`@Profile("demo")` + 配置 `app.demo.backfill=true`）中执行；已有同日期 run 则跳过。
- 结果：12+ 月趋势、四类归因；`project.duplicate_score` 最终为最近一次回填值；交付前再跑一次“当前日期”正式分析，保证最新分数与页面一致。

## 2. 附件与开源样例

```text
demo/attachments/
  README.md                     # 来源、许可证、获取日期、用途
  open-source/*.md|*.pdf        # 原始文档
  generated/*.docx              # 四段式申报书样例（POI 生成）
scripts/fetch-samples.ps1       # 下载 + 校验 + 失败回退
scripts/generate-sample-docx.ps1 或 Java DemoDocxGenerator
```

- 候选 raw 链接（固定 commit 或 master）：
  - `https://raw.githubusercontent.com/apache/shardingsphere/master/README.md`
  - `https://raw.githubusercontent.com/apache/dolphinscheduler/dev/README_zh_CN.md`
  - `https://raw.githubusercontent.com/apache/skywalking/master/README.md`
  - `https://raw.githubusercontent.com/apache/seatunnel/dev/README.md`
- 下载失败时：使用仓库内置的 `demo/attachments/open-source/fallback/*.md`（自写摘要，标注“离线生成”）。
- docx 生成：读取开源文档摘要 + 演示项目文本，按标题“一、项目目标 … 四、节点计划”写入 POI；用于解析演示。
- 许可证：Apache-2.0 等，保留 NOTICE/来源链接；不复制长段落进入申报数据，仅作解析样例。

## 3. 数据库与备份

```text
db/
  mysql/schema.sql        # 与 Flyway V1–V5 等价的可执行 DDL（utf8mb4）
  mysql/data.sql          # 角色/字典/演示项目/快照（可选，mysqldump 导出）
  h2/README.md            # H2 文件位置与复制方式
  backup/fund-platform-20261029.zip   # 实际备份产物（H2 data + uploads）
scripts/
  backup-db.ps1           # mysqldump（MySQL）或压缩 data/（H2）
  restore-db.ps1
```

- 备份前停止应用（H2 文件锁）；`CHECKPOINT` 后压缩。
- 恢复：解压到 `data/`，启动校验行数与登录。
- MySQL：`mysqldump --single-transaction --routines --triggers`；恢复 `mysql < schema.sql` + `data.sql`。
- 备份文件大小控制在 50MB 内；不含密码明文（BCrypt）。

## 4. 文档架构

```text
README.md
docs/
  requirements-analysis.md   # 背景、角色、流程、用例、功能/非功能
  design.md                  # 架构、模块、算法、时序、部署
  database.md                # 表、索引、ER、口径
  api.md                     # 接口、鉴权、错误码、示例
  user-guide.md              # 4 角色操作手册 + 截图
  metrics.md                 # 指标公式与示例
  demo-data.md               # 数据分布、账号、开源来源
  demo-script.md             # 5 分钟演示分镜
  submission-checklist.md    # 交付清单与门户填写
  screenshots/*.png
```

- 图表用 Mermaid（GitHub 可渲染）；截图使用 Playwright。
- 文档中的数字从演示数据实际计算结果导出，不手写估算。
- 版本与日期写入页脚；与代码同仓库维护。

## 5. 部署

### 5.1 单机 H2

```powershell
mvn -q -DskipTests package
java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo
# http://localhost:8080
```

### 5.2 前端静态资源

- 构建后复制 `frontend/dist/*` 到 `src/main/resources/static/`，Maven 重新打包，实现单 jar 提供页面 + API。
- 或在 Docker 中用 Nginx 托管，反代 `/api`。

### 5.3 Docker Compose

```yaml
services:
  mysql: mysql:8.0, utf8mb4, healthcheck, volume
  app:   build ., depends_on mysql healthy, env DB_URL/JWT_SECRET, port 8080
```

- `Dockerfile`：`maven:3.8-jdk-8` 构建 → `eclipse-temurin:8-jre` 运行；非 root 用户。
- `.env.example` 列出全部变量；`.env` 忽略。

### 5.4 WeCode

- 记录部署方式（Docker/镜像/命令）、端口、环境变量、首次启动日志、访问地址。
- 验证：健康检查 200、登录成功、趋势页有数据、上传解析成功。
- 若环境限制无法部署，记录尝试过程、错误与替代方案（本地 H2 + 截图/视频）。

## 6. 截图与演示

- Playwright 脚本步骤：登录 admin → dashboard → 项目列表 → 项目详情 → 分析报告 → 趋势 → 投入 → 预检 → 决策记录 → 系统管理。
- 每页等待图表渲染完成（`waitForSelector` + 500ms），关闭动画。
- 截图命名 `01-login.png` … `12-system.png`；检查无 Token/路径。
- 演示脚本分镜（约 5 分钟）：
  1. 0:00 问题与目标（PPT/首页）
  2. 0:30 申报书结构化与附件解析
  3. 1:30 重复度分析与解释
  4. 2:30 趋势与归因（重点：我方/他组/同期）
  5. 3:30 投入态势与风险敞口
  6. 4:00 新项目预检与报告
  7. 4:45 技术与交付说明

## 7. 最终质量流程

```text
1) mvn clean test / package
2) npm ci / npm run build
3) 启动 demo，执行端到端回归清单
4) Playwright 截图 + 控制台错误检查
5) 权限矩阵逐角色验证
6) 备份/恢复演练
7) 仓库扫描：密钥、密码、绝对路径、大文件、node_modules/data/storage
8) 更新 docs 与提交清单，记录验证时间
```

## 8. 风险

- 网络访问开源文档失败：内置 fallback，文档标注。
- 回填耗时：60 项目 × 14 月，单次 <1s，总量可接受；失败可重跑。
- 本机无法验证 Docker/MySQL：如实记录，提供 compose 配置与 WeCode 验证步骤，避免虚假声明。
- 截图含敏感信息：仅用演示数据；截图前检查 URL、账号、Token。
- 文档与实现漂移：交付前按清单逐项对照页面与代码。

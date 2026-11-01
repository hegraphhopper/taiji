# 自投研发项目重复投入治理平台 — 实施计划（父任务）

## 1. 执行原则

- 父任务只做需求基线、跨子任务契约与最终集成评审；功能实现落在 8 个子任务。
- 顺序执行（单人单机），前置子任务验收通过后再启动下游；UI 与后端可在一个子任务内并行推进，但必须一起验收。
- 核心主线优先：结构化申报书 → 重复度分析 → 趋势归因；角色体系与 OA 审批流后置。
- 鉴权基础设施（登录、JWT、角色枚举、SecurityContext、数据范围钩子、审计写入）在 foundation 先做，避免后期返工；管理界面与审批流程后置。
- 每个子任务结束必须：`mvn test` 通过、前端 `npm run build` 通过（涉及前端时）、更新对应 `prd.md` 验收勾选、记录实际验证结果。
- 不提交真实密钥、真实申报材料、个人敏感信息。

## 2. 环境事实（已核实）

| 项 | 事实 |
|----|------|
| OS / Shell | Windows，PowerShell 5.1 |
| JDK | 1.8.0_65（`D:\JDK`） |
| Maven | 3.5.0 |
| Node / npm | v24.12.0（`D:\SoftWare\tools\node`） |
| Python | 不可用（仅 Microsoft Store 占位），Trellis 脚本无法运行，任务文件手工维护 |
| MySQL / Docker | 本机未安装 |
| 网络 | Maven Central、raw.githubusercontent.com 可访问；GitHub API 403 |
| 现有工程 | 仅 `com.example:helloworld` 骨架，无框架/前端/数据库 |

因此：演示必须能在 H2 模式零依赖启动；MySQL 与 Docker 交付脚本需在 WeCode 环境验证。

## 3. 子任务执行顺序与门禁

### 阶段 A：核心主线（先做）

| 顺序 | 子任务 | 产出 | 进入条件 | 完成门禁 |
|------|--------|------|----------|----------|
| A1 | `10-29-fund-foundation` | 工程骨架、Flyway、最小登录与鉴权基础设施、种子、审计写入、前端壳 | 本计划获批 | 应用可启动；admin 登录；种子组/字典可用；数据范围钩子与审计写入生效；`mvn test`、`npm run build` 通过 |
| A2 | `10-29-fund-declaration` | 项目与申报书结构化、附件解析、基础状态流转 | foundation 验收 | “创建→解析→提交→管理员确认”通过；非法流转与越权被拒；详情可供引擎消费 |
| A3 | `10-29-fund-dup-engine` | 混合重复度引擎、配置、运行、快照 | declaration 验收 + 演示数据 | 固定夹具相似度符合预期；LLM 降级可用；300 项目 ≤5s |
| A4 | `10-29-fund-trend-attribution` | 趋势、归因、下钻、热力图 | engine 验收 | 四类归因与明细一致；筛选联动；下钻可见重复文本 |

### 阶段 B：增强价值（其次）

| 顺序 | 子任务 | 产出 | 进入条件 | 完成门禁 |
|------|--------|------|----------|----------|
| B1 | `10-29-fund-investment` | 投入态势、风险敞口 | A2 + A3 | 指标与明细复算一致；多维筛选正确 |
| B2 | `10-29-fund-decision` | 预检、建议、决策记录、Word/Excel | A3 | 三档建议规则正确；报告可打开且与页面一致 |

### 阶段 C：治理收尾（最后）

| 顺序 | 子任务 | 产出 | 进入条件 | 完成门禁 |
|------|--------|------|----------|----------|
| C1 | `10-29-fund-governance` | 四类角色细粒度权限、用户/组/字典管理、审计查询、形式审查、专家评审、审批流 | A + B 验收 | 权限矩阵全通过；完整审批流可演示；越权与非法流转被拒 |

### 阶段 D：交付

| 顺序 | 子任务 | 产出 | 进入条件 | 完成门禁 |
|------|--------|------|----------|----------|
| D1 | `10-29-fund-delivery` | 演示数据、文档、备份、截图、部署 | 全部功能验收 | 全新环境按 README 跑通；备份可恢复；交付清单齐全 |

顺序说明：A4 完成后即可进行第一次内部演示与评审；B 阶段可与 C 阶段交换顺序，但 C 不得早于 A4，D 必须在最后。A2 需要少量演示数据（建议 12–20 个项目）支撑引擎验证，完整 60 项目数据由 D1 生成。

## 4. 里程碑与验证命令

### 4.1 通用构建

```powershell
mvn -q -DskipTests package
mvn test
java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

cd frontend
npm install
npm run build
npm run dev
```

### 4.2 集成验收脚本（人工 + 自动化）

```powershell
# 后端启动后
Invoke-RestMethod -Uri http://localhost:8080/api/v1/health
# 登录（种子账号）
$body = @{username='admin'; password='Admin@123'} | ConvertTo-Json
$token = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType 'application/json' -Body $body).data.token
# 示例：分析运行
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/analysis/runs -Headers @{Authorization="Bearer $token"} -ContentType 'application/json' -Body '{"scope":"ALL"}'
```

- 前端浏览器验收使用 Playwright：登录 → 各页面截图 → 控制台无错误。
- 后端接口契约以 springdoc 页面为准，交付前导出一份 `docs/api.md` 摘要。

### 4.3 每个子任务的验证重点

| 子任务 | 自动化 | 人工/Playwright |
|--------|--------|-----------------|
| foundation | 登录、JWT、数据范围钩子、审计写入、Flyway 启动 | 登录、菜单、种子组/字典 |
| declaration | 基础状态流转、越权、解析器单测 | 表单、上传 docx/pdf、回填、提交 |
| dup-engine | 相似度夹具、权重、归因、LLM mock、性能 | 分析运行、报告页、相似对对比 |
| trend-attribution | 聚合口径、筛选、月份去重单测 | 折线/堆叠/热力图、筛选、下钻 |
| investment | 指标复算、口径、空数据单测 | KPI、图表、下钻 |
| decision | 规则映射、报告生成、降级单测 | 预检、决策记录、下载 Word/Excel |
| governance | 权限矩阵、状态机全转移、评审单测 | 用户/组/字典管理、审查、专家评分、审批流 |
| delivery | 启动脚本、备份恢复校验 | 全流程演示、截图、视频脚本 |

## 5. 风险与回滚点

| 风险 | 触发信号 | 处理 |
|------|----------|------|
| Spring Boot 2.7 与 Maven 3.5/POI 版本不兼容 | 依赖解析失败/编译报错 | 降级到 Boot 2.6.15 或调整 POI 版本；在 foundation 阶段一次性锁定 |
| H2 与 MySQL DDL 不兼容 | Flyway 在某一环境失败 | 移除方言特性，拆分为 `db/migration/common` + `db/vendor/mysql` 可选脚本 |
| 中文相似度效果差 | 固定夹具分数偏离预期 | 调整 n-gram/术语权重；引入同义词表；用大模型语义分补足 |
| 趋势页数据对不上 | 快照与明细计数不一致 | 以 `analysis_pair` 为唯一事实源，重建快照；加一致性测试 |
| 大模型调用不稳定/超时 | 分析耗时过长或失败 | 默认关闭；熔断 + 超时 + 缓存；报告标注“本地算法结果” |
| 前端图表在评审环境空白 | ECharts 初始化/尺寸问题 | 统一 ChartContainer，监听 resize；提供表格兜底 |
| 时间不足 | 子任务延期 | 保核心 A 阶段（foundation/declaration/engine/trend）与最小交付；B 可降级为最小闭环；C 治理收尾可按“角色只读 + 审批简化”降级，但权限校验不可缺失 |

回滚点：

- 每个子任务开始前在 git 打一次工作提交（或记录当前 commit），失败时 `git reset --hard <checkpoint>`，仅回滚未验收子任务。
- 数据库：演示库删除 `./data` 由 Flyway 重建；生产使用备份文件恢复。
- 配置：分析配置版本化，回滚只需将上一版本置为 active。

## 6. 最终集成评审清单

- [ ] 所有 P0/P1 子任务验收勾选完成，`check.jsonl` 引用的规范均已遵守。
- [ ] AC-01 ~ AC-12 逐条演示通过，记录截图或视频时间点。
- [ ] 指标口径文档化：重复度、等级、归因、风险敞口、同比。
- [ ] 权限矩阵逐角色验证（管理员/基金办/项目组/专家）。
- [ ] 大模型开启与关闭两种模式各跑一次分析，结果与降级标记正确。
- [ ] 数据库备份文件可在空库恢复并登录。
- [ ] README/需求/设计/用户手册与实际功能一致，无过期截图。
- [ ] 仓库无密钥、无大数据文件、`.gitignore` 覆盖 `data/`、`storage/`、`node_modules/`、`dist/`。
- [ ] 代码仓库、应用地址、数据库备份、文档四项交付物齐备。

## 7. 任务状态管理说明

- 因本机无 Python，`task.py start/archive` 不可用；以 `task.json` 的 `status` 字段手工维护（planning → in_progress → completed），并在子任务 `prd.md` 记录实际完成时间。
- 用户批准本计划后，将父任务与 `10-29-fund-foundation` 置为 `in_progress`，再进入实现。
- A4 完成后做一次内部演示，确认核心叙事成立，再进入 B/C；如核心效果不达标，先修算法与数据，不急着补角色与审批流。

## 8. 大模型收口执行清单（v1.2）

- [x] 1. 调整默认解释层：`AnalysisConfigService` 默认 100/0；`V4` 种子 100/0；新增 `V8__llm_explanation_default.sql` 迁移旧库。
- [x] 2. 前端默认值与提示：`AnalysisConfigView.vue` 默认 100/0，提示解释层口径。
- [x] 3. `LlmClient` 有界重试：超时/连接异常/5xx 最多 2 次尝试，4xx/非法 JSON 不重试；熔断按调用计数。
- [x] 4. 更新 `LlmFallbackTest`：新增 UT-32~UT-36，调整熔断用例的响应序列。
- [x] 5. 配置可复现：`.env.example` 补 LLM 变量；`start-demo.ps1` 自动加载 `application-local.yml`。
- [x] 6. 文档对齐：README（版本、解释层 100/0、重试、限制）、`docs/user-guide.md`、`docs/submission-checklist.md`、交付报告。
- [x] 7. 验证：`mvn -q test`（重点 `LlmFallbackTest`、`AnalysisConfigServiceTest`、`ScoreFusionTest`）；`npm run typecheck`；`npm run build`。
- [x] 8. 提交：`git add` 本次变更与 `docs/review/`，提交后推送 `origin`；推送失败需记录错误并告知用户。

### 8.1 回滚点

- 代码回滚：`git revert <llm-closure-commit>`；V8 仅影响默认权重，回滚后配置恢复 70/30。
- 数据回滚：演示库删除 `data/` 由 Flyway 重建。

### 8.2 手工维护说明

本机 `python.exe` 返回 9009（不可用），`task.py` 无法执行；`test.jsonl`、`task.json` 状态与归档按仓库既有约定手工维护。

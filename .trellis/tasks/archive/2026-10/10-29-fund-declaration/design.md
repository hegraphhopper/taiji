# 项目与申报书管理 — 技术设计

> 首版实现：项目主数据、结构化内容、附件解析、最小状态闭环（DRAFT/SUBMITTED/APPROVED/REJECTED）。完整状态机、形式审查、专家评审与审批流作为 governance 阶段目标保留在本设计中。

## 1. 数据模型（Flyway V3，紧随 foundation 的 V1/V2）

```sql
project(
  id, code, name, fund_type, group_id, owner_id, tech_direction_id,
  budget DECIMAL(14,2), start_date DATE, end_date DATE, status VARCHAR(20),
  summary TEXT, expected_outcome TEXT, duplicate_score DECIMAL(5,2) NULL,
  risk_level VARCHAR(10) NULL, submit_time DATETIME NULL,
  created_by, created_at, updated_at, deleted, version INT
)
project_goal(id, project_id, content TEXT, sort INT)
project_content(id, project_id, content TEXT, sort INT)
project_key_tech(id, project_id, name, description TEXT, difficulty, sort INT)
project_milestone(id, project_id, name, plan_start DATE, plan_end DATE,
                  deliverable TEXT, weight INT, sort INT)
project_member(id, project_id, user_id, member_role, workload DECIMAL(6,2))
project_document(id, project_id, file_name, stored_name, file_type, file_size,
                 storage_path, text_content LONGTEXT, parse_status,
                 parse_message, uploaded_by, created_at)
project_review(id, project_id, reviewer_id, round INT, score DECIMAL(5,2),
               opinion TEXT, result, created_at)
project_approval_log(id, project_id, from_status, to_status, action,
                     operator_id, comment, created_at)
```

索引：`project(code)` 唯一、`project(group_id,status)`、`project(tech_direction_id)`、`project(submit_time)`、子表 `project_id`、`project_review(project_id,reviewer_id)`。

`submit_time` 在首次提交时写入，撤回后保留；“重新提交”不覆盖首次提交时间（归因需要“在先”语义）。若业务要求以最新提交为准，可在配置中切换，默认首次。

## 2. 分层与组件

```text
project/
  controller/ ProjectController, ProjectDocumentController, ProjectReviewController
  service/    ProjectService, ProjectSectionService, ProjectWorkflowService,
              ProjectQueryService, ProjectDocumentService
  domain/     Project, ProjectGoal, ... （MyBatis-Plus 实体）
  mapper/     *Mapper
  dto/        ProjectSaveRequest, ProjectDetailVO, ProjectListItemVO,
              SectionSaveRequest, ReviewRequest, ApprovalRequest
  workflow/   ProjectStateMachine, ProjectAction
document/
  TextExtractor(docx/pdf/txt/md), SectionParser, DocumentStorage
```

- 写操作：`ProjectService.save`（主表 + 子表事务）、`ProjectWorkflowService.transition`（状态 + 轨迹 + 审计）。
- 读操作：`ProjectQueryService` 负责数据范围拼接、列表分页、详情聚合。
- DTO 与实体分离，禁止 Controller 直接返回实体。

## 3. 状态机

> 首版只实现 DRAFT/SUBMITTED/APPROVED/REJECTED 四态与 `submit/withdraw/approve/reject` 动作，由 ADMIN/PMO 执行；下表其余转移由 governance 启用。

```text
DRAFT      submit      -> SUBMITTED       GROUP_USER
SUBMITTED  withdraw    -> DRAFT           GROUP_USER
SUBMITTED  accept      -> REVIEWING       PMO
SUBMITTED  reject      -> REJECTED        PMO
REVIEWING  approve     -> APPROVED        PMO
REVIEWING  reject      -> REJECTED        PMO
REJECTED   resubmit    -> SUBMITTED       GROUP_USER
APPROVED   start       -> IN_PROGRESS     GROUP_USER
IN_PROGRESS complete   -> COMPLETED       GROUP_USER
IN_PROGRESS terminate  -> TERMINATED      PMO
APPROVED   terminate   -> TERMINATED      PMO
```

- 用枚举 + Map 定义合法转移；`ProjectAction` 携带所需角色、是否必填意见、前置校验。
- `transition()` 使用乐观锁 `version` 防并发；失败重试或提示刷新。
- 立项前检查：`project_review` 至少 1 条 `PASS`（配置 `requireExpertPass=true`）。
- 所有转移写 `project_approval_log`；`@Audit` 记录动作。

## 4. 附件解析

```text
upload -> 校验扩展名/大小/文件头 -> 落盘 storage/{yyyyMM}/{uuid}.{ext}
       -> 写 project_document(PENDING)
       -> 同步解析（≤20MB）
       -> TextExtractor -> 正文
       -> SectionParser -> Map<SectionType, List<String>>
       -> 更新 text_content / parse_status
       -> 返回预览给前端确认回填
```

- docx：`XWPFDocument` + `XWPFWordExtractor`；按段落读取，保留标题层级用于章节识别。
- pdf：`PDDocument` + `PDFTextStripper`，`setSortByPosition(true)`；加密/无文本层时抛出明确异常。
- txt/md：按 UTF-8 读取，BOM 处理；md 去除代码块与链接语法。
- SectionParser：
  - 标题正则：`^\s*(第?[一二三四五六七八九十]+[、.]|\d+[、.．]|【)?\s*(项目目标|研究目标|总体目标|研制内容|研究内容|关键技术|节点计划|进度安排|实施计划|考核指标)`。
  - 命中后收集到下一个标题前的内容；空行不截断。
  - 未识别到目标/内容时，整体正文作为“研制内容”候选并在 `parse_message` 提示。
- 文本清洗：去 BOM、统一换行、压缩连续空行、截断 200,000 字符。
- 安全：原始文件名仅展示，不使用其拼路径；下载按 ID 查 `storage_path` 并校验在根目录内；`Content-Disposition` 文件名 URL 编码。

## 5. 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects` | 分页筛选 |
| POST | `/api/v1/projects` | 创建草稿 |
| GET | `/api/v1/projects/{id}` | 详情聚合 |
| PUT | `/api/v1/projects/{id}` | 基本信息 |
| PUT | `/api/v1/projects/{id}/sections` | 目标/内容/技术/节点/成员批量保存 |
| DELETE | `/api/v1/projects/{id}` | 删除草稿 |
| POST | `/api/v1/projects/{id}/actions` | 统一流转 `{action, comment}` |
| GET | `/api/v1/projects/{id}/logs` | 审批轨迹 |
| GET/POST | `/api/v1/projects/{id}/reviews` | 评审列表/提交 |
| POST | `/api/v1/projects/{id}/documents` | 上传（multipart） |
| POST | `/api/v1/documents/{id}/parse` | 重新解析 |
| GET | `/api/v1/documents/{id}/download` | 下载 |
| DELETE | `/api/v1/documents/{id}` | 删除 |
| GET | `/api/v1/projects/{id}/documents/{docId}/sections` | 解析预览 |

统一动作接口优于多个 URL，便于前端与权限集中校验；动作非法返回 `40010 PROJECT_ILLEGAL_TRANSITION`。

## 6. 前端

- `/projects`：筛选栏（关键词、组、基金、方向、状态、预算区间）、表格、批量提交/撤回、新建按钮。
- `/projects/new`、`/projects/:id/edit`：分区表单 + 右侧锚点；附件区支持拖拽上传与解析预览弹窗（分区展示可回填内容，勾选后应用）。
- `/projects/:id`：概览卡片（预算/状态/负责人/重复度）、Tab 内容、操作按钮按状态与角色渲染。
- 评审弹窗：评分滑块/输入、意见、结论；提交后刷新详情。
- 审批动作统一二次确认；驳回意见必填。
- 使用 Pinia `projectEditStore` 保存草稿表单状态（路由离开提醒），提交成功后清理。
- 角色与状态共同决定按钮可见性；后端仍校验。

## 7. 测试

- 状态机：8 状态 × 全部动作矩阵，合法/非法各断言。
- 越权：GROUP_USER 访问他组详情/编辑/删除；EXPERT 编辑；非法角色审批。
- 解析：docx（含标题）、pdf（文本层）、无文本层 PDF、乱码 txt、超大文件。
- 事务：批量保存中途失败回滚，子表不残留。
- 集成：MockMvc 走完提交→审查→评审→立项，校验轨迹与审计。
- 性能：1000 行列表查询 < 1s（H2 近似验证）。

## 8. 风险

- PDF 中文抽取顺序可能错乱：启用 `setSortByPosition`，失败时提示人工校对。
- 子表整体替换会丢 ID：重复度不依赖子表 ID，可接受；审计只记项目级变更。
- 附件同步解析阻塞请求：20MB 上限 + 解析超时（30s）保护；超时置 FAILED。
- 项目删除策略：仅草稿可删；被分析引用后禁止物理删除，保持历史一致性。

# 项目与申报书管理契约（A2）

## 1. Scope / Trigger

修改项目主数据、结构化申报内容、状态流转、附件上传/解析、项目列表数据范围时适用。

## 2. Signatures

### 项目接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/projects` | 登录 | 分页筛选 |
| POST | `/api/v1/projects` | ADMIN/PMO/GROUP_USER | 创建草稿 |
| GET | `/api/v1/projects/{id}` | 按数据范围 | 详情聚合 |
| PUT | `/api/v1/projects/{id}` | 按数据范围 | 基本信息 |
| PUT | `/api/v1/projects/{id}/sections` | 按数据范围 | 结构化内容整体替换 |
| DELETE | `/api/v1/projects/{id}` | 按数据范围 | 仅草稿 |
| POST | `/api/v1/projects/{id}/actions` | 见动作 | SUBMIT/WITHDRAW/APPROVE/REJECT |
| GET | `/api/v1/projects/{id}/logs` | 按数据范围 | 审批轨迹 |

### 附件接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/v1/projects/{id}/documents` | 可编辑项目 | multipart `file` |
| POST | `/api/v1/documents/{id}/parse` | 可编辑项目 | 重新解析 |
| GET | `/api/v1/documents/{id}/download` | 可查看项目 | 文件流 |
| DELETE | `/api/v1/documents/{id}` | 可编辑项目 | 删除元数据与文件 |
| GET | `/api/v1/projects/{id}/documents/{docId}/sections` | 可查看项目 | 章节预览 |

### 关键类

```text
ProjectService.create/update/delete(ProjectSaveRequest)
ProjectSectionService.save(Long, SectionSaveRequest)
ProjectWorkflowService.submit/withdraw/approve/reject(Long, ProjectActionRequest)
ProjectQueryService.list(ProjectQuery) / detail(Long)
ProjectDocumentService.upload/parse/preview/download/delete
DocumentTextExtractor.extract(byte[], extension): String
SectionParser.parse(String): SectionPreviewVO
```

## 3. Contracts

### 项目主数据

- 状态：`DRAFT / SUBMITTED / APPROVED / REJECTED`（A2）；完整枚举保留。
- 编码：`TJJH-{yyyy}-{4 位序号}`，可手工覆盖且全局唯一。
- 预算 `DECIMAL(14,2)`，必须 >0；结束日期不得早于开始日期。
- `submit_time` 首次提交写入，撤回/重提不覆盖。

### 结构化内容

- 整体替换：请求中某列表为 `null` 时跳过，`[]` 表示清空。
- 子表 `sort` 缺省按数组顺序；提交时要求目标、研制内容、节点各至少 1 条。
- 成员角色：`LEADER/CORE/PARTICIPANT`；关键技术难度：`HIGH/MEDIUM/LOW`。

### 状态流转

```text
DRAFT     SUBMIT   -> SUBMITTED   GROUP_USER/ADMIN/PMO
SUBMITTED WITHDRAW -> DRAFT       提交人（或 ADMIN/PMO）
SUBMITTED APPROVE  -> APPROVED    ADMIN/PMO
SUBMITTED REJECT   -> REJECTED    ADMIN/PMO，comment 必填
```

- 非法转移 → 400 `40010`；无权限 → 403；审批轨迹逐条落库。

### 附件

- 类型：docx/pdf/txt/md；单文件 ≤20MB；每项目 ≤10 个。
- docx 校验 ZIP 头，pdf 校验 `%PDF-`；文件名只取 basename，不使用客户端路径。
- 存储：`{app.upload.dir}/{yyyyMM}/{uuid}.{ext}`；数据库存相对路径。
- 解析：docx 用 POI、pdf 用 PDFBox（文本层）、txt/md 按 UTF-8；正文截断 200,000 字符。
- 章节关键词：项目目标/研究目标/总体目标/研制内容/研究内容/关键技术/节点计划/进度安排/实施计划/考核指标。
- 解析失败置 `FAILED` 并记录 `parse_message`，不阻断上传。

## 4. Validation & Error Matrix

| 条件 | HTTP | 错误码 |
|------|------|--------|
| 编码重复 | 400 | `40020` |
| 非草稿/驳回修改或删除 | 400 | `40021` |
| 项目不存在 | 404 | `40410` |
| 非法流转 | 400 | `40010` |
| 跨组访问/编辑 | 403 | `40301` |
| 附件类型不支持 | 400 | `40030` |
| 附件超过 20MB | 400 | `40031` |
| 附件超过 10 个 | 400 | `40032` |
| 附件不存在 | 404 | `40420` |
| 提交缺少目标/内容/节点 | 400 | `40000` |
| 驳回无意见 | 400 | `40000` |
| 乐观锁冲突 | 400 | `40000`（提示刷新） |

## 5. Good / Base / Bad

- Good：group user 创建本组草稿，保存目标/内容/节点，提交后 PMO 立项，轨迹完整。
- Base：管理员可查看全量；group user 仅本组；提交后不可编辑，撤回后恢复草稿。
- Bad：只靠前端隐藏按钮，不校验后端归属。
- Bad：附件使用原始文件名拼路径，造成路径穿越。
- Bad：上传先落库后校验，导致失败请求残留元数据/文件。

## 6. Tests Required

- `ProjectServiceTest`：编码生成/唯一/手工编码、预算日期、乐观锁。
- `ProjectSectionServiceTest`：排序、整体替换、事务回滚。
- `ProjectWorkflowServiceTest`：提交校验、四类动作、非法流转、角色、轨迹。
- `ProjectQueryServiceTest`：跨组、筛选、排序、分页。
- `ProjectDeleteAndAuditTest`：草稿删除、审计。
- `ProjectDocumentApiTest`：docx/pdf/txt/md 解析、类型/大小/数量、下载、预览、正文截断。
- 全量 `mvn -q test` 必须通过（A2 完成后 46/46）。

## 7. Wrong vs Correct

### Wrong：子表保存无事务

```java
goalMapper.delete(...);
goalMapper.insert(goal);
milestoneMapper.insert(badMilestone); // 抛异常，目标已被清空
```

### Correct

```java
@Transactional
public void save(Long id, SectionSaveRequest request) {
    replaceGoals(...);
    replaceMilestones(...); // 任一异常整体回滚
}
```

### Wrong：路径直接拼接

```java
Path target = Paths.get(storageRoot, document.getStoragePath());
```

### Correct

```java
Path target = storageRoot.resolve(relativePath).normalize();
if (!target.startsWith(storageRoot)) {
    throw new BizException(ErrorCode.BAD_REQUEST, "附件路径非法");
}
```

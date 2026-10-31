# 项目与申报书管理 — 实施计划

## 1. 顺序清单

- [ ] Flyway `V3__project_schema.sql`，H2 启动验证；同步准备 MySQL 兼容检查。
- [ ] 实体、Mapper、枚举（FundType/ProjectStatus/MemberRole/ReviewResult/ParseStatus）。
- [ ] `ProjectQueryService`：数据范围、筛选、分页、详情聚合。
- [ ] `ProjectService`：创建/更新/删除、编码生成、子表批量保存事务。
- [ ] `ProjectStateMachine` + `ProjectWorkflowService`：首版 submit/withdraw/approve/reject，轨迹与审计；完整评审流转留到 governance。
- [ ] `ProjectDocumentService`：存储、上传校验、解析、预览、下载、删除。
- [ ] `TextExtractor`（docx/pdf/txt/md）、`SectionParser`、清洗工具。
- [ ] 首版不实现专家评审 Service；`project_review` 表可随 V3 建好，接口后置。
- [ ] Controller + DTO + 参数校验 + OpenAPI 注解。
- [ ] 后端测试：基础状态流转、越权、解析夹具、事务、集成流程。
- [ ] 前端：项目列表、创建/编辑分区表单、详情 Tab、附件上传与解析预览、评审弹窗。
- [ ] 前端：状态/角色按钮控制、离开提醒、空态/错误态。
- [ ] 端到端演示脚本：创建 → 上传 → 回填 → 提交 → 审查 → 评审 → 立项。

## 2. 验证命令

```powershell
mvn -q clean package
mvn test

# 启动 demo
java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

# 登录并创建项目（示例）
$h = @{Authorization="Bearer $token"}
$p = @{ name='面向工业质检的多模态缺陷识别'; fundType='PLATFORM'; groupId=1;
        techDirectionId=1; budget=1200000; startDate='2026-11-01';
        endDate='2027-10-31'; summary='...' } | ConvertTo-Json
$proj = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/projects `
  -Headers $h -ContentType 'application/json' -Body $p

# 上传附件（PowerShell 5.1 需用 .NET 构造 multipart）
# 前端手工验证：拖拽 demo/attachments/sample-declaration.docx，查看解析预览

cd frontend
npm run build
npm run dev
```

- 负向验证：`user02` 访问 `user01` 的项目详情返回 403；`DRAFT` 项目直接调用 `approve` 返回 40010。
- 解析验证：记录 docx/pdf 解析出的四段内容长度与预览截图。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| `V3__project_schema.sql` | 与 V1/V2 冲突或方言不兼容 | 仅回滚未发布 migration；演示库重建 |
| `ProjectStateMachine` | 转移矩阵遗漏导致流程卡死 | 以测试矩阵为准；保留 `DRAFT` 重置入口（管理员） |
| `TextExtractor` | 大文件 OOM | 限制 20MB/200k 字符 + 超时；异常置 FAILED |
| 子表批量保存 | 部分写入 | 单事务 + 单测覆盖中途异常 |
| 附件目录 | 路径穿越/文件泄露 | 统一校验根目录；下载只接受 ID |

## 4. 完成定义（DoD）

- PRD 验收项全部勾选，附实际命令/截图说明。
- 状态机矩阵测试通过；越权用例通过；解析 4 种格式各 1 份样例通过。
- `mvn test`、`npm run build` 通过。
- 项目详情接口返回完整结构，可供重复度引擎直接消费（文本字段齐全）。
- 审计与审批轨迹在页面可查。

## 5. 交接给下游

- 引擎需要的输入契约：`ProjectAnalysisInput{projectId, groupId, submitTime, goalText, contentText, techText, milestoneText, tags}`，由 `ProjectQueryService` 提供。
- 附件解析文本统一存入 `project_document.text_content`，引擎按项目聚合时优先使用结构化字段，缺失时回退附件文本。

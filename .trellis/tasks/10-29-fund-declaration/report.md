# 开发报告 — A2 项目与申报书管理

## 成果摘要

A2 已完成自投项目的结构化申报闭环：项目主数据与编码、目标/研制内容/关键技术/节点/成员、附件上传与 docx/pdf/txt/md 解析、章节预览与一键回填、基础状态流转与审批轨迹、列表筛选与数据范围、审计。前端新增项目列表、分区表单、详情六个 Tab 与附件操作。A2 新增 24 条用例全部通过，全量 46/46。

## 已完成需求

- Flyway `V3__project_schema.sql`：9 张项目相关表、唯一编码与查询索引。
- 项目主数据：创建/更新/删除、`TJJH-年-4 位` 自动编码、手工编码唯一、预算与日期校验、乐观锁。
- 结构化内容：整体替换、排序、难度/成员角色校验、事务回滚。
- 状态流转：提交（完整性校验）、撤回（仅提交人）、确认立项、驳回（意见必填）、非法流转 400、审批轨迹。
- 数据范围：ADMIN/PMO 全量，GROUP_USER 本组，EXPERT 已提交项目只读；列表与详情二次校验。
- 附件：docx/pdf/txt/md 上传、类型/文件头/20MB/10 个限制、UUID 存储、路径穿越防护、下载与删除。
- 解析：POI/PDFBox/UTF-8 提取、200,000 字符截断、章节识别、预览、回填；失败置 FAILED 并记录原因。
- 前端：项目列表筛选/分页/提交/撤回/删除、分区新建编辑表单、详情概览/申报内容/节点成员/附件/审批轨迹/重复度占位、解析预览与回填、驳回弹窗。
- 审计：项目创建/更新/删除、内容保存、状态动作、附件上传/解析/删除均接入 `@Audit`。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn -q test` | 46/46 通过，退出码 0（A1 22 + A2 24） |
| `mvn -q clean package` | 退出码 0，产出 jar |
| `npm.cmd run typecheck` / `run build` | 退出码 0，1700 模块 |
| 冷启动 | 清理 `data/`、`storage/` 后启动成功，Flyway V1–V3 全部执行 |
| 健康检查 | `/api/v1/health` HTTP 200，profile=demo，db=UP |
| 浏览器 | 列表空态、新建/编辑、详情六 Tab、附件上传/解析/预览/回填、状态动作与轨迹通过；正常流程 0 error |
| 数据范围 | user01 本组创建成功，跨组创建 403；列表仅本组 |
| 附件安全 | 伪造 docx、exe 均 400；下载内容 SHA-256 与源文件一致 |

## 关键实现说明

- 子表保存单事务，先删后插；解析同步执行，失败不影响上传主流程。
- 状态机采用显式判断而非工作流引擎；`submit_time` 首次写入后不覆盖。
- 前端 401/403 统一拦截；附件下载走 Blob + Authorization，避免 token 出现在 URL。
- 为测试独立配置 H2 内存库与 `target/test-storage`，不触碰生产 `data/`、`storage/`。

## 遗留问题与风险

- PDF 仅支持文本层，扫描件提示人工录入；未接入 OCR。
- 附件解析在请求线程同步执行，20MB 上限内可接受，后续可改异步任务。
- 专家评审、形式审查与完整审批流留待 governance。
- 项目成员暂以用户 ID 录入，治理阶段接入人员选择器。
- 项目列表前端主包约 1.13 MB（gzip 375 KB），Element Plus 全量引入；后续可优化分包。
- 测试数据已清空；当前为空库状态，交付阶段将写入 60 项目演示数据。

## 运行方式

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-demo.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/status-demo.ps1
```

- UI：http://127.0.0.1:5173
- 健康检查：http://localhost:8080/api/v1/health
- 账号：`admin / Admin@123`、`pmo01 / Pmo@12345`、`user01 / User@12345`

## 相关文档

- `.trellis/tasks/10-29-fund-declaration/prd.md`、`design.md`、`implement.md`
- `.trellis/tasks/10-29-fund-declaration/check.md`
- `.trellis/tasks/10-29-fund-declaration/test/用例设计.md`
- `.trellis/tasks/10-29-fund-declaration/test/unit/report-round-1.md`
- `.trellis/spec/backend/project-guidelines.md`

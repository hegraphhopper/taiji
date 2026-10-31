---
task_id: fund-declaration
result: passed
---
# 检查报告

## 检查范围

- A2 项目与申报书管理：项目主数据/编码、结构化内容批量保存与事务回滚、状态流转与审批轨迹、数据范围与列表筛选、草稿删除、审计、附件上传/解析/下载与安全限制。
- 依据 `test/用例设计.md` 的 UT-01 ~ UT-24，覆盖正常、边界、异常三类场景。
- 测试源码：`src/test/java/com/taiji/fund/project/`（6 个测试类 + 1 个测试基类）。
- 环境：Spring Boot Test + H2 内存库 + MockMvc，`@ActiveProfiles({"demo","test"})`；附件写入 `target/test-storage`。

## 命令与结果

| 命令 | 结果 |
|------|------|
| `mvn -q -DskipTests test-compile` | 退出码 0，测试源码编译通过 |
| `mvn -q test` | 退出码 0；全量 46 项测试通过（A1 基线 22 + A2 新增 24），Failures 0、Errors 0、Skipped 0 |
| `mvn -q test -Dtest=ProjectServiceTest,...` | 新增 6 个测试类执行通过（首轮唯一失败由测试脚本排序假设导致，修正后复测通过） |

## 问题及修复

- `ProjectQueryServiceTest.ut13`：预算区间查询使用默认 `updatedAt desc` 排序却按预算倒序断言。修正为区间查询显式传 `sortBy=budget&sortOrder=desc`；仅调整测试脚本假设，未修改生产代码、未删断言、未放宽期望。
- 未发现 A2 生产代码缺陷，无需交回 `trellis-test-rework` 修复轮。

## 复测与证据

- 首轮报告：`test/unit/report-round-1.md`（24/24，通过率 100%，阈值 100%，轮次 1/5）。
- 首轮最终全量运行：`mvn -q test` 退出码 0；surefire 统计：
  - ProjectServiceTest 5 项、ProjectSectionServiceTest 2 项、ProjectWorkflowServiceTest 7 项
  - ProjectQueryServiceTest 2 项、ProjectDeleteAndAuditTest 2 项、ProjectDocumentApiTest 6 项
  - A1 基线 22 项继续通过
- 未使用 skip/only/@Disabled，无调试输出残留；未执行 `git commit`。

## 未执行项及原因

- 未执行 `mvn clean` / `mvn package`、`scripts/start-demo.ps1` 运行态验证、前端 `npm run build`：本轮单元测试职责与派发约束明确禁止这些操作，且不属于 UT-01 ~ UT-24 的设计范围。
- A2 前端页面交互（列表/表单/附件拖拽等）不在本次单元测试用例设计范围内，未执行浏览器验收。

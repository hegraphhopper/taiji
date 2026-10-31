---
task_id: fund-dup-engine
result: passed
---
# 检查报告（A3 重复度分析）

## 检查范围

- 后端：V4 迁移、分析配置、分词/向量/打分/归因/片段、LLM 增强与降级、运行编排与并发、快照聚合与回写、查询接口与数据范围。
- 前端：分析运行页、分析配置页、项目详情「重复度」Tab、路由与菜单角色显隐。
- 依据 `test/用例设计.md` UT-01 ~ UT-29；对应测试类见 `src/test/java/com/taiji/fund/analysis/`。

## 命令与结果

| 命令 | 结果 |
|------|------|
| `mvn -q -DskipTests compile` | 退出码 0 |
| `mvn -q test` | 退出码 0；113 项全部通过，Failures 0 / Errors 0 / Skipped 0 |
| `mvn test "-Dtest=AnalysisPerformanceTest" "-Dtest.excludedGroups=none"` | 1 项通过；300 项目 FULL 耗时 1531ms，pairCount=1450（门禁 5s） |
| `mvn -q clean package` | 退出码 0，产出 `target/fund-platform-1.0.0.jar`（63,147,688 字节） |
| `npm.cmd run typecheck` | 退出码 0 |
| `npm.cmd run build` | 退出码 0（主包 1,131 KB / gzip 375 KB，仅体积告警） |

## 运行态验证

1. 空库冷启动：`scripts/start-demo.ps1 -SkipBuild` → backend/frontend 双 UP；`pmo01` 触发 FULL → `SUCCESS`、`projectCount=0`、`pairCount=0`；`admin` 读取配置 version=1、权重 15/35/35/5/10。
2. 造数验证（3 个已提交项目，其中 A/C 跨组近重复）：
   - FULL 运行 `SUCCESS`，`projectCount=3`、`pairCount=1`（无序对只存一条）。
   - A 报告：`max=99.80`、`HIGH`、`relation=CROSS_GROUP`、`direction=SIMULTANEOUS`（同日提交）、五维 100/100/100/95.97/100、matched=3、diff=0。
   - 快照：`highPairCount=1`、`crossGroupCount=1`、`simultaneousCount=1`；敞口 1,197,600 = 预算 1,200,000 × 99.8%，可复算。
   - 预检（`user01`，四段文本）：返回 3 个候选，55.00/LOW/EXPLAIN、55.00/LOW/EXPLAIN、17.56/NONE/APPROVE；草稿分数未被回写。
   - 预检缺少文本时返回 400 `40043`（校验生效）。
3. 并发/失败清理/权限由自动化用例覆盖（`AnalysisConcurrencyTest`、`AnalysisFailureTest`、`AnalysisAccessTest`、`RunDeleteTest`）。

## 问题及修复

- 端到端造数脚本在 Windows PowerShell 5.1 下中文乱码：脚本需保存为 UTF-8 BOM；属验证工装问题，不改产品代码。
- `ProjectAccessService` 新增 `canView(project)` 只读判断，供项目对明细「双方任一可见」使用；原 `assertCanView` 行为不变（A2 用例全部通过）。
- 未发现 A3 生产代码缺陷；未触发修复轮。

## 未执行项及原因

- 真实大模型联调：无 API Key 与可达的 OpenAI 兼容端点，仅以桩覆盖超时/500/非法 JSON/合法融合/熔断/缓存；默认关闭不影响演示。
- MySQL 8 实库验证：本机无 MySQL，DDL 按 H2/MySQL 兼容语法编写，需在 WeCode/交付环境复验。
- 交付级演示数据（约 60 项目）与截图/视频：归 `10-29-fund-delivery`。

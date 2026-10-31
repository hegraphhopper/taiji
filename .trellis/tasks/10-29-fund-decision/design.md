# 新项目辅助决策与报告导出 — 技术设计

## 1. 数据表（Flyway V5）

```sql
decision_rule(id, priority INT, min_score DECIMAL(5,2), max_score DECIMAL(5,2),
              relation VARCHAR(20) NULL, same_group TINYINT NULL,
              action VARCHAR(20), description, enabled TINYINT,
              created_at, updated_at)
decision_record(id, project_id NULL, run_id, risk_score DECIMAL(5,2),
                level VARCHAR(10), recommendation VARCHAR(20), reason TEXT,
                similar_projects TEXT, savings_estimate DECIMAL(14,2) NULL,
                input_hash VARCHAR(64), operator_id, created_at, deleted TINYINT)
```

- `similar_projects` 保存 Top-5 快照（含双方名称、组、分数、片段），保证报告可复现。
- `input_hash`：临时文本预检的 SHA-256 摘要，不存原文。
- 默认规则由 Flyway 种子写入。

## 2. 预检流程

```text
POST /decision/precheck {mode: PROJECT|TEXT, projectId?, text?}
  -> 权限与数据范围校验
  -> PROJECT: 构造 ProjectAnalysisInput，调用 AnalysisExecutor.precheck(project)
     TEXT: 构造临时 input（无 projectId），与存量项目比较
  -> 取 Top-5 pair（按 totalScore 降序）
  -> 规则引擎匹配 decision_rule（priority 升序，首个命中）
  -> savings = budget != null ? budget * score / 100 : null
  -> experts = 同方向、启用、进行中评审数升序 limit 3
  -> 组装 PrecheckResult（含 runId、规则命中 id、理由）
  -> 不落 decision_record（等待保存）
```

- 复用 engine 的 `PRECHECK` 运行类型；临时文本不写 `project`，但写 `analysis_run`（`trigger_type=PRECHECK`）便于审计与复现。
- `updateSnapshot=false`：预检结果不写 `project.duplicate_score`；趋势仍取正式运行快照。

## 3. 规则引擎

```java
DecisionRule match(PrecheckContext ctx) {
  return rules.stream()
    .filter(enabled)
    .filter(r -> ctx.score >= r.minScore && ctx.score < r.maxScore)
    .filter(r -> r.relation == null || r.relation == ctx.relation)
    .filter(r -> r.sameGroup == null || r.sameGroup == ctx.sameGroup)
    .sorted(by priority)
    .findFirst()
    .orElse(fallback EXPLAIN);
}
```

默认规则：

| 优先级 | 分数 | 条件 | 动作 |
|--------|------|------|------|
| 10 | 90–100 | 任意 | `TERMINATE` |
| 20 | 80–90 | 同组或对方在先 | `MERGE` |
| 30 | 60–100 | 任意 | `EXPLAIN` |
| 40 | 0–60 | 任意 | `APPROVE` |

- 分数比较使用落库两位小数；边界采用左闭右开，最后一条用 `<=100`。
- 规则命中结果与规则版本写入理由字符串，便于回看。
- 大模型理由仅在 `reason` 后追加一段，不改变 `recommendation`。

## 4. 建议专家

```sql
-- 同技术方向、启用、角色 EXPERT
-- 进行中评审数 = count(project_review where reviewer_id = u.id and project in REVIEWING)
-- 按数量升序、姓名排序，limit 3
```

- 无匹配：返回空数组，前端提示“暂无可推荐专家，请联系基金办”。
- 不返回手机号/邮箱等敏感信息。

## 5. 决策记录

- 保存时二次校验权限与项目归属；复制预检结果快照，不引用实时数据。
- 列表：项目、组、等级、建议、金额、操作人、时间；分页。
- 作废：`deleted=1` + 审计，不物理删除。
- 导出报告读取记录快照，不重新计算。

## 6. 报告生成（POI）

```text
report/
  WordReportBuilder     XWPFDocument，标题/段落/表格/页脚
  ExcelReportBuilder    SXSSFWorkbook
  ReportController
```

Word 结构：

1. 标题：“自投项目重复投入辅助决策报告”
2. 元信息表：项目名称/编码/项目组/负责人/预算/生成时间/报告编号
3. 结论区：风险分、等级、建议、预计重复投入、理由
4. 相似项目表：Top-5（名称、组、状态、总分、归因）
5. 重复点表：维度 / 关键词 / 片段
6. 差异点表
7. 口径与免责声明

- 中文字体：`宋体`/`微软雅黑`，设置 `eastAsia`；避免默认 Calibri 导致中文回退。
- 表格宽度、列宽固定；长文本自动换行。
- 文件名：`决策报告_{projectCode}_{yyyyMMdd}.docx`；响应头 `Content-Disposition` + RFC 5987 编码。
- Excel Sheet：`相似项目`、`重复点`、`差异点`、`口径`。

## 7. 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/decision/precheck` | 预检（PROJECT/TEXT） |
| POST | `/api/v1/decision/records` | 保存决策 |
| GET | `/api/v1/decision/records` | 记录列表 |
| GET | `/api/v1/decision/records/{id}` | 记录详情 |
| DELETE | `/api/v1/decision/records/{id}` | 作废（ADMIN/PMO） |
| GET | `/api/v1/decision/records/{id}/report.docx` | Word |
| GET | `/api/v1/decision/records/{id}/report.xlsx` | Excel |
| GET/PUT | `/api/v1/decision/rules` | 规则查询/保存（ADMIN） |

预检响应：

```json
{"runId":12,"riskScore":86.4,"level":"HIGH",
 "topPeers":[{"peerProjectId":7,"name":"...","groupName":"...","totalScore":86.4,
              "relation":"CROSS_GROUP","direction":"PEER_EARLIER",
              "scores":{"goal":..,"content":..,"tech":..,"milestone":..,"tag":..},
              "matchedPoints":[...],"diffPoints":[...]}],
 "recommendation":"MERGE","ruleId":20,"reason":"...",
 "savingsEstimate":1036800.00,"budget":1200000.00,
 "suggestedExperts":[{"id":5,"realName":"...","groupName":"..."}],
 "llmUsed":false}
```

## 8. 前端

- `/decision`：左侧预检表单（Tab：选择项目 / 粘贴文本），右侧结果。
- 结果区：风险仪表（ECharts gauge）、建议动作大标签、理由、金额、相似项目卡片、重复/差异折叠面板、专家列表。
- 保存决策按钮 → 弹窗确认 → 记录页。
- `/decision/records`：筛选表格、详情抽屉、下载 Word/Excel。
- 规则配置放在系统管理（管理员）。
- 颜色与等级映射统一：高 `#e5484d`、中 `#f5a524`、低 `#1f5eff`、无 `#8b93a7`。

## 9. 测试

| 测试 | 内容 |
|------|------|
| RuleEngineTest | 四档默认规则、边界 60/80/90、条件组合、回退 |
| SavingsTest | 预算×分数/100、无预算、四舍五入 |
| ExpertTest | 同方向、负载排序、无匹配 |
| PrecheckTest | PROJECT/TEXT、权限、不改变状态/快照 |
| RecordTest | 保存快照、不可编辑、作废、权限 |
| ReportTest | docx 可被 POI 重新读取、含关键章节、中文；xlsx 行列 |
| FallbackTest | LLM 失败仍返回规则结论 |

## 10. 风险

- 规则与 LLM 冲突：规则优先，LLM 只补理由，页面明确标注。
- 报告字体在不同环境缺失：设置 eastAsia 字体 + 回退字体；交付前在目标环境验证。
- 临时文本预检不落项目：记录中只存 hash 与摘要，避免隐私与存储膨胀。
- 预检运行可能与正式运行并发：复用 engine 的并发保护，预检独立 run 不影响正式快照。

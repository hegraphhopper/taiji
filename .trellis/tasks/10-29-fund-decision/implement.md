# 新项目辅助决策与报告导出 — 实施计划

## 1. 顺序清单

- [ ] Flyway `V5__decision_schema.sql` + 默认规则种子。
- [ ] 预检上下文与 DTO；复用 engine `precheck`。
- [ ] `DecisionRuleService`：规则加载、匹配、校验、保存、审计。
- [ ] 金额计算、建议专家查询。
- [ ] 预检 Controller 与权限/数据范围。
- [ ] `DecisionRecordService`：保存快照、列表、详情、作废。
- [ ] Word 报告生成器（字体、表格、页脚、文件名编码）。
- [ ] Excel 报告生成器（多 Sheet）。
- [ ] 导出接口与审计。
- [ ] 后端测试：规则、金额、专家、预检、记录、报告、降级。
- [ ] 前端预检页（项目/文本 Tab）、结果展示、保存。
- [ ] 前端记录页、详情、下载、规则配置页。
- [ ] 端到端演示：预检 → 保存 → 记录 → 下载报告。

## 2. 验证命令

```powershell
mvn -q clean package
mvn test -Dtest='*Decision*,*Rule*,*Report*'

java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

$body = @{ mode='PROJECT'; projectId=1 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/decision/precheck `
  -Headers $h -ContentType 'application/json' -Body $body

Invoke-WebRequest "http://localhost:8080/api/v1/decision/records/1/report.docx" -Headers $h -OutFile report.docx
Invoke-WebRequest "http://localhost:8080/api/v1/decision/records/1/report.xlsx" -Headers $h -OutFile report.xlsx

cd frontend
npm run build
```

- 校验：用 POI 重新读取生成的 docx，断言标题、相似项目表、口径段落存在。
- 边界：分数 60/80/90 的夹具分别命中 EXPLAIN/MERGE/TERMINATE。
- 降级：关闭 LLM 与错误 LLM 配置各跑一次，结论不变。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| `V5__decision_schema.sql` | 规则种子冲突 | 只增不改；重建演示库 |
| `DecisionRuleService` | 规则误命中 | 优先级 + 单测覆盖；保留回退 EXPLAIN |
| 报告生成 | 中文乱码/文件损坏 | eastAsia 字体 + POI 回读测试 |
| 决策记录 | 引用实时数据导致漂移 | 保存完整快照 JSON |
| 预检并发 | 与正式运行冲突 | 复用 engine 并发控制 |

## 4. 完成定义（DoD）

- 四种建议动作在夹具下均正确命中；金额与专家建议符合规则。
- 决策记录可保存、查询、回看、作废；报告可打开且内容一致。
- LLM 开启/关闭/失败三种情况均可用，结论稳定。
- 权限与数据范围验证通过；预检不改变项目状态与趋势。
- 后端测试、`npm run build` 通过；演示截图与报告样例入交付。

## 5. 交接给下游

- 交付子任务使用真实的决策记录与报告样例做截图/演示。
- 报告口径说明复用 `docs/metrics.md`，避免多处措辞不一致。

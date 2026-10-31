# 交付报告（进行中）

## D0 演示数据基线（2026-10-31 完成）

### 成果

- `DemoDataInitializer` 替换旧 8 项目 seeder：60 个项目、2024–2026 提交时间、8 个项目组、7 种状态、预算 30–268 万。
- 近似主题 10 对：同组 2 对、跨组间隔 3 个月 6 对、跨组 10 天内 2 对；其余 40 个独立主题。
- 每个项目含 1 目标、2 研制内容、3 关键技术、3 节点、2 成员。
- 开关 `DEMO_DATA_SEED`（默认 true），`test` profile 跳过，`DEMO-` 前缀幂等。

### 验证

| 项 | 结果 |
|----|------|
| 项目总数 | 60 |
| 状态覆盖 | DRAFT 4 / SUBMITTED 8 / REVIEWING 4 / APPROVED 18 / REJECTED 4 / IN_PROGRESS 18 / COMPLETED 4 |
| 项目组 | 8 个组均有项目（7–8 个/组） |
| 趋势点 | 14（2025-08 ~ 2026-09） |
| 四类归因 | self 32 / our 143 / peer 143 / simultaneous 18 |
| 归因合计 | 336 = Σ pairCount |
| 草稿排除 | 分析项目数 56 = 60 − 4 DRAFT |
| 重启幂等 | 项目数仍为 60 |
| 回归测试 | `mvn -q test` 136/136 通过 |

### 遗留（D 阶段）

- 20+ 项目附件元数据与 4–6 份样例文档（docx/pdf/md）
- MySQL 脚本、备份恢复、文档、截图、Docker/WeCode 部署

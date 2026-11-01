# 数据库设计

## 1. 迁移

Flyway 位于 `src/main/resources/db/migration`，按 `V1`–`V7` 顺序执行；H2 与 MySQL 8 共用语法。

| 版本 | 内容 |
|------|------|
| V1 | 系统表：`sys_group/sys_user/sys_role/sys_user_role/tech_direction/audit_log` |
| V2 | 核心种子：4 角色、8 项目组、12 技术方向、4 演示账号 |
| V3 | 项目与申报书：`project`、目标/内容/关键技术/节点/成员/附件/评审/审批轨迹 |
| V4 | 分析：`analysis_config`、`analysis_run`、`analysis_pair`、`duplicate_snapshot`、`llm_call_log` |
| V5 | 决策：`decision_rule`、`decision_record` |
| V6 | 系统参数 `system_config`、`project.review_round`、评审轮次索引 |
| V7 | `analysis_pair.llm_overlaps`、`analysis_pair.llm_differences` |

## 2. 核心表

### project

`code/name/fund_type/group_id/owner_id/tech_direction_id/budget/start_date/end_date/status/submit_time/duplicate_score/version/review_round/deleted`。

状态：`DRAFT/SUBMITTED/REVIEWING/APPROVED/REJECTED/IN_PROGRESS/COMPLETED/TERMINATED`。

### analysis_pair

`run_id/project_id/peer_project_id` + 五维分 + `local_score/llm_score/total_score/level/relation/direction` + `matched_points/diff_points/llm_reason/llm_overlaps/llm_differences`。

### duplicate_snapshot

按 `analysis_date` 保存项目快照：最高分、Top 平均、高风险对数、跨组/同组/我方在先/他方在先/同期计数、风险敞口。

### llm_call_log

`run_id/project_id/peer_project_id/model/status/latency_ms/prompt_tokens/completion_tokens/error`；不记录 prompt/Key/正文。

## 3. 索引

- `analysis_pair(run_id, project_id, total_score)`、`analysis_pair(peer_project_id)`。
- `duplicate_snapshot(project_id, analysis_date)`、`(group_id, analysis_date)`、`(run_id)`。
- `analysis_run(created_at/status/project_id)`。
- `project_review(project_id, round)`。

## 4. 口径

- 趋势/归因以 `duplicate_snapshot` + `analysis_pair` 为事实源，可按月复算。
- 历史回填 `analysis_date` 为月末，不回写项目当前分数。
- 同一项目对只保留一个方向，展示时按查看项目视角翻转。
- 软删除 `deleted`；唯一编码需考虑软删后复用问题。

## 5. 备份

- H2：`scripts/backup-db.ps1` / `restore-db.ps1`，`db/backup/` 提供实际备份。
- MySQL：`db/mysql/schema.sql`（V1–V7）、`data.sql`（演示数据占位），见 `db/mysql/README.md`。

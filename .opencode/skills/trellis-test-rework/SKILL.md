---
name: trellis-test-rework
description: "测试修复专家。主会话在 trellis-unit-test 第一轮通过率未达 test_pass_threshold 后派发独立的 trellis-test-rework subagent：其内部自持修复轮循环（修复→重跑→写 report-round-N.md，N 达 max_fix_rounds 仍未达标则停止并交回主会话），界定修复边界、禁止通过削弱测试来修绿。Use when 第一轮单元测试未达标、需要进入修复轮时。"
---

# Trellis 测试修复（Test Rework）

本 skill 由**主会话**在 `trellis-unit-test` 第一轮（`report-round-1.md`）通过率未达 `test_pass_threshold` 后派发 `trellis-test-rework` subagent 时使用。`trellis-test-rework` 是独立 sub-agent，其内部自持修复轮循环。

## 触发时机

- 当前任务状态：in_progress，Phase 2.2（check 阶段测试环节）。
- `trellis-unit-test` 第一轮返回通过率 < `test_pass_threshold`（`.trellis/config.yaml`，默认 1.0）。

## 主会话派发协议

通过 Task 工具分发 `trellis-test-rework` subagent：

- **Agent type**：`trellis-test-rework`
- **Dispatch prompt 首行必须是**：`Active task: <TASK_DIR>`（插件据此注入 test.jsonl / 用例设计 / prd/design/implement / 已有报告上下文）
- 随后说明：你已是 `trellis-test-rework` subagent，直接执行修复轮循环（先读 `.trellis/config.yaml` 的 `test_pass_threshold` 与 `max_fix_rounds`，当前轮次 N 从 `test/unit/report-round-*.md` 已有文件数推导），不得再次分发任何 subagent。

## 修复边界（谁可以修）

| 修复对象 | 测试修复 agent |
|---|---|
| 测试脚本自身错误（断言写错、mock 不对、用例设计理解偏差） | ✅ 自修 |
| 产品代码缺陷（实现不符合用例期望） | ✅ 可自修 |
| 用例设计的预期结果（迁就代码改期望） | ❌ 禁止 |
| 超出测试职责的大范围重构 / 新增功能 | ❌ 写入报告交回主会话 |

## 修复禁止项（禁止"修绿"）

- 不得删除断言、放宽期望、把失败用例改为跳过来提升通过率（`it.skip` / `describe.skip` / 整文件 `only` / 跳过整个测试套件）。
- 不得修改 `test/用例设计.md` 的预期结果来迁就代码。
- 不得通过规避测试路径（提前 return、mock 掉被测逻辑）制造假通过。
- 不得超出 `max_fix_rounds`（默认 5）继续自修；超限时停止并交回主会话。

## 修复轮循环（由 subagent 内部执行）

```
Round N: 分析失败用例（测试脚本问题 / 产品代码缺陷 / 环境问题）
         → 修复（优先修测试脚本自身错误；确属产品代码缺陷时修复产品代码）
         → 重跑（只针对相关用例或全量，统计通过率）
         → 生成当轮报告 {TASK_DIR}/test/unit/report-round-N.md
         通过率 ≥ test_pass_threshold → 结束
         未达标 且 N < max_fix_rounds → 进入 Round N+1
         N 达 max_fix_rounds 仍不达标 → 停止，剩余失败交回主会话
```

- 当前轮次 N 从 `{TASK_DIR}/test/unit/report-round-*.md` 已有文件数推导（如已有 `report-round-1.md`，本轮为 Round 2）。
- 报告沿用 `trellis-unit-test` 的报告格式（执行命令 / 用例总数 / 通过 / 失败 / 跳过 / 通过率 / 失败明细 / 修复说明）。
- 产物始终落到 `{TASK_DIR}/test/unit/`，不得写到其他目录。

## 主会话收尾校验

subagent 返回后，主会话核对：

- [ ] `{TASK_DIR}/test/unit/report-round-*.md` 齐全（round-1 由 unit-test、后续轮由 test-rework 产生）
- [ ] 轮次 ≤ max_fix_rounds（默认 5）；通过率未达标时失败已交回主会话且未继续自修
- [ ] 无"修绿"痕迹（无删断言 / skip / 改用例期望）
- [ ] 通过或失败结果已纳入最终完成汇报

若发现 subagent 违规（超轮次、产物缺失、未交回失败、修绿），在汇报中明确指出并让主会话决策。

## 收尾

`trellis-test-rework` subagent 修复完成后向主会话汇报：修复的文件清单（`文件:行` 与原因）、轮次与通过率、剩余失败清单（如有）。不得执行 `git commit`。


## Required task check report

At the start of quality checking, create or update `<TASK_DIR>/check.md` with `result: in_progress` BEFORE running checks. Testing, repair and re-test stages must update the same report and mark it in_progress while work continues. Finish by recording passed, failed, blocked, or skipped according to actual evidence. Never invent successful checks. Resolve TASK_DIR from the current session binding, never from a previous task.

Use this exact metadata format (replace TASK_ID with the task.json id):

```markdown
---
task_id: TASK_ID
result: in_progress
---
# 检查报告
## 检查范围
## 命令与结果
## 问题及修复
## 复测与证据
## 未执行项及原因
```

Include the actual commands and outcomes, unresolved issues, each re-test round and links to `test/unit/report-round-*.md` and applicable API reports. Summarize rather than copying logs. For skipped, include a line `跳过原因：` followed by the concrete reason. Parent tasks summarize their children. At finish-work, verify this report matches the final verification before archive. Archive rejects absent, invalid, mismatched or in_progress reports, including with --force. A failed/blocked report may be archived truthfully; archive does not certify quality. Keep check.md under 256 KiB and inside the task directory.

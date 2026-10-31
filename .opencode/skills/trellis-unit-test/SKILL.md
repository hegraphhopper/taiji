---
name: trellis-unit-test
description: "单元测试专家。在 check 阶段（trellis-check 之后）使用：依据 test/用例设计.md 分发 trellis-unit-test subagent 编写并执行第一轮单元测试，写 report-round-1.md 并返回通过率；未达 test_pass_threshold 时再分发 trellis-test-rework subagent 进入修复轮（test_pass_threshold 默认 100%、max_fix_rounds 默认 5 轮）。测试脚本与报告留存到 {TASK_DIR}/test/unit/。Use when 代码已实现并通过 check、需要单元级测试验证时。"
---

# Trellis 单元测试（Unit Test）

在 check 阶段、`trellis-check` 之后加载本 skill。负责对本次代码变更执行单元级测试检查。测试验证分**两阶段**：`trellis-unit-test` 做第一轮，`trellis-test-rework` 做修复轮。

## 触发时机

- 当前任务状态：in_progress，Phase 2.2。
- `trellis-check`（spec 合规与自修复）已执行完毕。

## 前置检查

1. 确认用例设计存在：
   - 有 `{TASK_DIR}/test/用例设计.md` → 以其 UT 用例为准。
- 用例设计缺失：停止并交回主任务调用 trellis-use-case-design 补齐，不得现场推导代替设计。
2. 确认项目测试约定：读取 `.trellis/spec/` 中对应包/层的测试 spec（如 `unit-test/index.md`、`conventions.md`），测试脚本必须沿用项目现有框架与风格，不硬编码、不自创风格。

## 阶段一：分发 trellis-unit-test（第一轮）

通过 Task 工具分发 `trellis-unit-test` subagent：

- **Agent type**：`trellis-unit-test`
- **Dispatch prompt 首行必须是**：`Active task: <TASK_DIR>`（插件据此注入 test.jsonl / 用例设计 / prd/design/implement 上下文）
- 随后说明：你已是 `trellis-unit-test` subagent，直接执行**第一轮**单元测试（先读 `.trellis/config.yaml` 的 `test_pass_threshold` 与 `max_fix_rounds`），写 `report-round-1.md` 并返回通过率与失败清单，不得进入修复轮、不得再次分发任何 subagent。

第一轮契约（subagent 执行）：

```
Round 1: 依据用例设计编写测试脚本到 {TASK_DIR}/test/unit/ → 运行 → 统计通过率
         → 生成 {TASK_DIR}/test/unit/report-round-1.md
         → 返回：通过率、失败用例清单、是否达标
```

## 阶段二：主会话判阈值，未达标派发 trellis-test-rework

`trellis-unit-test` 返回后，主会话读取 `report-round-1.md` 判定：

- 通过率 ≥ `test_pass_threshold`（默认 1.0）→ **结束**，报告齐全。
- 未达标 → 分发 `trellis-test-rework` subagent 进入修复轮（详见 `trellis-test-rework` skill）。其内部自持修复轮循环，直到达标或轮次超过 `max_fix_rounds`。

## 报告格式（第一轮，中文）

```markdown
# 单元测试报告 Round 1

## 执行信息
- 执行命令：<实际运行的测试命令>
- 用例总数：X｜通过：Y｜失败：Z｜跳过：S
- 通过率：<P>%｜阈值：<test_pass_threshold×100>%｜轮次：1/max_fix_rounds

## 失败用例明细
| 用例ID | 期望 | 实际 | 原因/堆栈摘要 |
```

## 主会话收尾校验

全部测试环节（阶段一 + 阶段二）完成后，主会话核对：

- [ ] `{TASK_DIR}/test/unit/` 下测试脚本与报告齐全，报告按轮次命名（round-1 由 unit-test、后续轮由 test-rework 产生）
- [ ] 轮次 ≤ max_fix_rounds（默认 5）；通过率未达标时失败已交回主会话且未继续自修
- [ ] 测试遵循项目约定（框架/命名/目录）
- [ ] 测试脚本本身无调试残留、无跳过整个测试套件的写法（`it.skip` / `describe.skip` 全量跳过）
- [ ] 通过或失败结果已纳入最终完成汇报

若发现 subagent 违规（超轮次、产物缺失、未交回失败、修绿），在汇报中明确指出并让主会话决策。

## 与原生 trellis-check 的关系

- `trellis-check`：spec 合规、自修复、lint/typecheck。
- 本 skill：补充测试覆盖验证，顺序为 check → unit-test（→ test-rework 如未达标）。


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

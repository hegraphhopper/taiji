# Finish Work

## 输出语言

面向用户的确认、归档摘要、会话记录的标题、summary 和正文使用中文；命令、字段、固定路径、提交哈希和必要技术术语保持原样。下文英文示例只说明调用结构，执行时填写中文内容。

Wrap up the current session: archive the active task (and any other completed-but-unarchived tasks the user wants to clean up) and record the session journal. Code commits are NOT done here — those happen in workflow Phase 3.4 before you invoke this command.

## Step 1: Survey current state

```bash
python.exe ./.trellis/scripts/get_context.py --mode record
```

This prints:

- **My active tasks** — review whether any besides the current one are actually done (code merged, AC met) and should be archived this round.
- **Git status** — quick visual on what's dirty.
- **Recent commits** — you'll need their hashes in Step 4 for `--commit`.

If `--mode record` surfaces other completed tasks not tied to the current session, surface them to the user with a one-shot confirmation: "These N tasks look done — archive them too in this round? [y/N]". Default is no; the current active task is always archived in Step 3 regardless.

## Step 2: Sanity check — classify dirty paths

Run:

```bash
git status --porcelain
```

Filter out paths under `.trellis/workspace/` and `.trellis/tasks/` — those are managed by `add_session.py` and `task.py archive` auto-commits and will appear dirty as part of this skill's own work.

For each remaining dirty path, decide whether it belongs to **the current task** or to **other parallel work** (e.g., another terminal window editing the same repo). Heuristics:

- Paths referenced in the current task's `prd.md` / `implement.jsonl` / `check.jsonl` → current task
- Paths in code areas matching the task's stated scope, or that you remember editing this session → current task
- Paths in unrelated areas you have no recollection of touching this session → other parallel work

Then route:

- **Any remaining path looks like current-task work** — bail out with:
  > "Working tree has uncommitted code changes from this task: `<list>`. Return to workflow Phase 3.4 to commit them before running `/trellis:finish-work`."

  Do NOT run `git commit` here. Do NOT prompt the user to commit. The user goes back to Phase 3.4 and the AI drives the batched commit there.
- **All remaining paths look unrelated** (other parallel-window work) — report them once and continue to Step 3:
  > "FYI, dirty files outside this task's scope — leaving them for the other window: `<list>`."
- **Genuinely unsure** — ask the user once: "Are `<list>` this task's work I forgot to commit, or another window's? (commit / ignore)" — then route per their answer.

## Step 3: Archive task(s)

Before archiving the active task, follow “Required task development report” below: generate or update report.md from the task's cumulative implementation and final check.md in the main session.

Before archiving, confirm with the user. Present the task(s) you plan to archive and ask one-shot:

> "The following task(s) are complete: `<list>`. Archive them now? [y/N]"

Default is no — do NOT archive unless the user confirms. Only after the user confirms, run:

```bash
python.exe ./.trellis/scripts/task.py archive <task-name>
```

At minimum: the current active task (if any). Plus any extra tasks the user confirmed in Step 1. Each archive produces a `chore(task): archive ...` commit via the script's auto-commit.

If the user does not confirm, skip the archive and leave the task(s) in `in_progress`. If there is no active task and the user did not confirm any cleanup archives, skip this step.

## Step 4: Record session journal

```bash
python.exe ./.trellis/scripts/add_session.py \
  --title "本次会话标题" \
  --commit "hash1,hash2" \
  --summary "本次会话摘要"
```

Use the work-commit hashes produced in Phase 3.4 (visible in Step 1's `Recent commits` list, or via `git log --oneline`) for `--commit`. Do not include the archive commit hashes from Step 3. This produces a `chore: record journal` commit.

Final git log order: `<work commits from 3.4>` → `chore(task): archive ...` (one or more) → `chore: record journal`.

+## Required task development report

During the existing verification and finish phase, the MAIN agent responsible for wrapping up must create or update `<TASK_DIR>/report.md` after the final verification result is known and before archiving. Do not add a workflow step, dispatch a reporting agent, or make a separate model request for this report.

Resolve TASK_DIR from the current session binding (`task.py current`), and confirm its task.json id. This report belongs to the TASK, like check.md: all sessions working on that task update the same file, and it moves with the task directory on archive. If no task is bound, do not guess another task's directory or manufacture a report.

Read the task's prd.md, applicable design.md / implement.md, existing report.md, actual changes and final check.md. Rewrite report.md as the task's CURRENT CUMULATIVE outcomes, rather than appending session logs. Preserve still-relevant confirmed work from other sessions. For parent tasks, summarize confirmed child outcomes without claiming unfinished children are complete.

Use this fixed Markdown structure and concise Chinese business language:

```markdown
# 开发报告

## 成果摘要
简要说明本任务目前实现了什么。

## 已完成需求
- 已完成的功能及其实际效果。

## 遗留问题
- 尚未完成的需求、已知问题或待确认事项。
- 没有已知遗留问题时如实说明。

## 验证结论
简述已验证的范围、结果及未验证的部分。
详细验证依据见[验证报告](check.md)。
```

Replace every placeholder with actual evidence. Distinguish implementation from verification: an implemented feature is not automatically verified; a successful command is not proof that every requirement works. Failed, blocked, skipped or unperformed checks must remain explicit. Do not invent completion, successful checks or absence of issues. Avoid file lists, commands, outputs and technical activity logs in this business report.

On every later wrap-up of the same task, update this same report to match the latest requirements, implementation and check.md. Keep it inside TASK_DIR and under 256 KiB. Existing reports remain visible until updated; the client displays the file modification time. report.md is a wrap-up obligation with a missing-report UI hint, NOT a new archive hard gate. Historical tasks without this report are not backfilled automatically.

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

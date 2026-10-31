---
name: trellis-check
description: "Comprehensive quality verification: spec compliance, lint, type-check, tests, cross-layer data flow, code reuse, and consistency checks. Use when code is written and needs quality verification, before committing changes, or to catch context drift during long sessions."
---

# Code Quality Check

Comprehensive quality verification for recently written code. Combines spec compliance, cross-layer safety, and pre-commit checks.

---

## Step 1: Identify What Changed

```bash
git diff --name-only HEAD
git status
```

## Step 2: Read Task Artifacts and Applicable Specs

Read the current task artifacts in order:

- `prd.md`
- `design.md` if present
- `implement.md` if present

```bash
python.exe ./.trellis/scripts/get_context.py --mode packages
```

For each changed package/layer, read the spec index and follow its **Quality Check** section:

```bash
cat .trellis/spec/<package>/<layer>/index.md
```

Read the specific guideline files referenced — the index is a pointer, not the goal.

## Step 3: Run Project Checks

Run the project's lint, type-check, and test commands. Fix any failures before proceeding.

## Step 4: Review Against Checklist

### Code Quality

- [ ] Linter passes?
- [ ] Type checker passes (if applicable)?
- [ ] Tests pass?
- [ ] No debug logging left in?
- [ ] No suppressed warnings or type-safety bypasses?

### Test Coverage

- [ ] New function → unit test added?
- [ ] Bug fix → regression test added?
- [ ] Changed behavior → existing tests updated?

### Spec Sync

- [ ] Does `.trellis/spec/` need updates? (new patterns, conventions, lessons learned)

> "If I fixed a bug or discovered something non-obvious, should I document it so future me won't hit the same issue?" → If YES, update the relevant spec doc.

## Step 5: Cross-Layer Dimensions (if applicable)

Skip this step if your change is confined to a single layer.

### A. Data Flow (changes touch 3+ layers)

- [ ] Read flow traces correctly: Storage → Service → API → UI
- [ ] Write flow traces correctly: UI → API → Service → Storage
- [ ] Types/schemas correctly passed between layers?
- [ ] Errors properly propagated to caller?

### B. Code Reuse (modifying constants, creating utilities)

- [ ] Searched for existing similar code before creating new?
  ```bash
  grep -r "pattern" src/
  ```
- [ ] If 2+ places define same value → extracted to shared constant?
- [ ] After batch modification, all occurrences updated?

### C. Import/Dependency (creating new files)

- [ ] Correct import paths (relative vs absolute)?
- [ ] No circular dependencies?

### D. Same-Layer Consistency

- [ ] Other places using the same concept are consistent?

---

## Step 6: Report and Fix

Report violations found and fix them directly. Re-run project checks after fixes.


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

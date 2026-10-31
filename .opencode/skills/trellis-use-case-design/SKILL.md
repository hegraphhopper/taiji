---
name: trellis-use-case-design
description: "用例设计专家。在 plan 阶段需求收敛后、最终评审前使用：依据 prd/design/implement 编写单元用例，产出 {TASK_DIR}/test/用例设计.md 与 test.jsonl 清单，作为 check 阶段 trellis-unit-test subagent 的测试输入。Use when 规划产物已完成、需要为后续测试准备用例时。"
---

# Trellis 用例设计（Use Case Design）

在 plan 阶段、需求收敛后、最终评审前加载本 skill。产出物直接成为 check 阶段 `trellis-unit-test` 的测试输入。

## 目标产物

1. `{TASK_DIR}/test/用例设计.md` — 用例设计文档
2. `{TASK_DIR}/test.jsonl` — 上下文清单（指向用例设计文档 + 相关测试 spec），供 check 阶段测试 subagent 注入

## 前置条件

- 任务处于 planning 阶段，`prd.md` 已收敛（需求与验收标准明确），复杂任务已有 `design.md` / `implement.md`。
- 尚未进入最终评审 / `task.py start`。

## 流程

### 第 1 步：读取输入

按序读取：`{TASK_DIR}/prd.md` → `{TASK_DIR}/design.md`（如有）→ `{TASK_DIR}/implement.md`（如有）。从中提取：

- 验收标准（AC）与需求条目 → 每个 AC/需求至少映射一个用例
- 接口面：命令、HTTP API、模块公共函数/类方法（来自 design.md 的边界与契约）
- 边界条件与异常路径（错误处理、空值、超限、权限、并发）

### 第 2 步：设计用例

设计**单元用例（UT）**，针对函数/类/模块的单一行为：
- 正常路径：主流程
- 边界路径：边界值、空输入、极值
- 异常路径：非法输入、异常抛出、错误返回

> 接口级行为（HTTP/RPC、CLI 命令、模块公共 API）以单元用例形式覆盖被测模块对外的契约逻辑，不再单独设计接口用例区。

每条用例字段（模板）：

```markdown
### UT-01 <用例标题>
- 关联需求/AC：<R1 / AC1>
- 被测对象：<函数/模块:签名 或 文件路径>
- 前置条件：<mock、数据准备>
- 输入：<参数值>
- 步骤：<1. 2. 3.>
- 预期结果：<可断言的期望值>
- 优先级：P0 / P1 / P2
```

用例 ID 必须稳定：`UT-NN` 递增编号，全程不得改号，后续报告引用它。

### 第 3 步：创建目录并落盘

```bash
mkdir -p <TASK_DIR>/test
```

写入 `<TASK_DIR>/test/用例设计.md`，包含：

- 用例设计摘要（被测范围、测试优先级说明）
- 单元用例区（UT）
- 用例-需求/AC 映射表

### 第 4 步：种 test.jsonl

用 `task.py add-context` 登记上下文（顺序即注入顺序）：

```bash
python.exe ./.trellis/scripts/task.py add-context <TASK_DIR> test test/用例设计.md "测试用例设计（check 阶段测试输入）"
python.exe ./.trellis/scripts/task.py add-context <TASK_DIR> test .trellis/spec/<package>/<layer>/unit-test/index.md "项目测试约定"
```

至少登记用例设计文档一条；测试 spec 视任务领域补充。

## 质量门槛

- 每个验收标准 / 需求条目至少映射一个用例，映射表可追溯。
- 单元用例覆盖正常/边界/异常三类。
- 用例 ID 稳定唯一；预期结果可断言（不是"应该正常"这类空话）。
- `test.jsonl` 存在且至少含用例设计文档一条。


## 编码前交付要求

报告必须非空，test.jsonl 必须引用当前任务的 test/用例设计.md。报告以如下元数据开头：
```yaml
---
task_id: 当前task.json中的id
result: ready
---
```
不适用时 result: not_applicable，增加 reason 并在正文解释；不代表测试通过。始终通过 skill 工具显式调用本技能，不以直接写文档代替活动记录。

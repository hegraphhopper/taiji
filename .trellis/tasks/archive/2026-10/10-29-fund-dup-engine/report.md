# 开发报告（A3 重复度分析引擎）

## 成果摘要

A3 打通「结构化申报 → 可解释判重 → 项目级快照」：运行级 TF-IDF 与五维加权打分给出 0–100 分与四档等级，中性归因落库、按查看视角翻译，重复/差异片段指回原文；可选大模型只复核 Top-N 且在超时/异常时降级；运行支持全量/单项目/预检，具备 409 并发保护、失败清理、`analysisDate` 历史回填。前端新增分析运行页、分析配置页，项目详情「重复度」Tab 由占位改为真实报告。

## 已完成需求

- F1 分析配置：版本自增、旧版本失效、严格校验、仅 ADMIN 修改并审计。
- F2 文本处理：归一化、2-gram + 英文词 + 术语词典（310 条，权重 ×3）、运行级 TF-IDF。
- F3 五维打分与归因：goal/content/tech 余弦、milestone 名称+时间重叠、tag Jaccard；同组/他组在先/我方在先/同期；片段与差异点。
- F4 LLM：OpenAI 兼容调用、严格 JSON 校验、Top-N 候选、24h 缓存、运行内熔断、`llm_call_log` 脱敏。
- F5 运行：FULL/PROJECT/PRECHECK、并发 409、失败清理、快照回写、analysisDate 权限与语料过滤。
- F6 查询：项目报告、项目对明细、运行历史/详情、删除失败运行；数据范围强制。
- F7 性能：300 项目全量 1.5s（门禁 5s）；落库 Top-K 并集。
- 前端：`/analysis/runs`、`/analysis/config`、菜单角色显隐、项目详情重复度 Tab。

## 验证结果

| 验证项 | 结果 |
|--------|------|
| `mvn -q test` | 113/113 通过 |
| 性能用例 | 300 项目 1531ms，pairCount=1450 |
| `mvn -q clean package` | 通过，jar 63.1 MB |
| `npm.cmd run typecheck` / `run build` | 通过 |
| 冷启动 + 空库 FULL | 成功，计数为 0 |
| 跨组近重复端到端 | 99.80/HIGH，归因与快照计数一致，敞口可复算 |
| 预检 | 返回 Top-3 与建议动作，不回写项目分数 |

## 关键实现说明

- `analysis_pair` 无序对只存一条，`pair_count` 为唯一对数；快照平均分用各自 Top-K，避免双计与口径漂移。
- 归因存储中性事实（`A_EARLIER/B_EARLIER/SIMULTANEOUS`），查询时按当前项目翻译为「我方/他组在先」。
- 分数落库保留 2 位，等级比较使用落库值，规避 79.996 显示为 80 的判定歧义。
- 失败清理按 `run_id` 删除 pair/snapshot；运行锁为进程内 `AtomicBoolean` + 运行表校验。
- 术语词典拆为纯 POJO + Spring 提供器，分词包不依赖容器；LLM 传输抽象便于桩测试。
- 性能用例 `@Tag("perf")` 默认被 surefire 排除，手动执行命令写入 spec 与 README。

## 遗留问题与风险

- 附件解析文本按整篇回退到 goal/content/tech 三个空维度，可能同质化放大相似度，后续建议按章节映射。
- 停用字为单字过滤，口径偏激进，需用交付演示语料校准。
- 配置版本 `max+1` 未加数据库锁，多实例并发保存可能撞唯一索引；演示单实例可接受。
- LLM 仅桩验证，真实端点字段差异与超时行为待在联调环境确认；缓存为进程内。
- 前端主包 1.13 MB（Element Plus 全量引入），交付前可做分包。
- 本机无 MySQL，V4 的 MySQL 8 执行需在 WeCode/交付环境复验。

## 相关文档

- `.trellis/tasks/10-29-fund-dup-engine/prd.md`、`design.md`、`implement.md`
- `.trellis/tasks/10-29-fund-dup-engine/test/用例设计.md`
- `.trellis/tasks/10-29-fund-dup-engine/check.md`
- `.trellis/spec/backend/analysis-guidelines.md`

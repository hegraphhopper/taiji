# 演示数据

## 1. 规模

- 60 个项目，编码 `DEMO-001`…`DEMO-060`，`DEMO-` 前缀幂等。
- 8 个项目组，2024–2026 提交时间。
- 状态分布：DRAFT 4 / SUBMITTED 8 / REVIEWING 4 / APPROVED 18 / REJECTED 4 / IN_PROGRESS 18 / COMPLETED 4。
- 每个项目：1 目标、2 研制内容、3 关键技术、3 节点、2 成员。

## 2. 重复模式

| 模式 | 对数 | 说明 |
|------|------|------|
| 同组高度重复 | 2 | 组内两项目，文本重合约 90% |
| 跨组、对方在先 | 3+3 | 提交相差约 3 个月，产生 PEER_EARLIER/OUR_EARLIER |
| 跨组、30 天内 | 2 | 同期并行 |
| 低重复/无关 | 其余 40 个 | 不同领域主题 |

趋势回填 14 个月（2025-08 ~ 2026-09），四类归因均 > 0；实测 self 32 / our 143 / peer 143 / simultaneous 18。

## 3. 初始化与开关

- `demo` profile 首次启动自动生成；`DEMO_DATA_SEED=false` 可关闭；`test` profile 跳过。
- 重复启动按 `DEMO-` 前缀跳过，不重复插入。
- 趋势回填：`scripts/backfill-trend.ps1 -Months 14`。

## 4. 账号

见 `README.md` 与 `docs/user-guide.md`；`expert02/expert03` 用于满足默认 2 票立项演示。

## 5. 附件样例

- 当前演示数据未生成真实附件文件；`project_document` 元数据与解析能力已具备。
- 开源样例与 docx 生成列入 D 阶段遗留，不影响核心演示（项目文本已结构化）。

## 6. 数据边界

- 全部为模拟数据，不含真实申报材料、个人信息或密钥。
- 密码为 BCrypt 哈希；演示账号仅本地使用。

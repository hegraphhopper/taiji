# 提交清单

## 1. 四项交付物

| 交付物 | 状态 | 位置/说明 |
|--------|------|-----------|
| 应用访问地址 | 待部署 | 本地 `http://127.0.0.1:5173`；WeCode 地址未获取，D5 阻塞 |
| 代码仓库 | 本地已提交，远程待授权 | `origin` 已配置；GitHub 写权限未通过 |
| 数据库备份 | 已完成 | `db/backup/fund-platform-20261101-100939.zip` |
| 需求分析与设计文档 | 已完成 | `docs/requirements-analysis.md`、`docs/design.md`、`docs/database.md`、`docs/api.md` 等 |

## 2. 文档

- [x] `README.md`：启动、账号、目录、配置、FAQ
- [x] `docs/requirements-analysis.md`
- [x] `docs/design.md`
- [x] `docs/database.md`
- [x] `docs/api.md`
- [x] `docs/user-guide.md`
- [x] `docs/metrics.md`
- [x] `docs/demo-data.md`
- [x] `docs/demo-script.md`
- [x] `docs/A阶段产品设计书.md`

## 3. 代码与验证

- [x] `mvn -q test`：174/174
- [x] `mvn -q -DskipTests package`
- [x] `npm run typecheck` / `npm run build`
- [x] 四角色权限与数据范围走查（16 张截图）
- [x] 大模型解释层、连接自检、预检解释、报告复核段
- [ ] 真实大模型联调：需注入 `LLM_BASE_URL/LLM_API_KEY/LLM_MODEL`
- [ ] Docker Compose 实际运行：本机无 Docker，仅提供配置
- [ ] WeCode 部署：环境/入口未提供

## 4. 数据库

- [x] H2 备份与解压校验
- [x] `db/mysql/schema.sql`（V1–V7）
- [ ] MySQL 8 实际导入验证（本机无实例）

## 5. 截图

- `docs/screenshots/governance/`：16 张，四角色与系统管理
- `docs/screenshots/core/`：8 张（工作台、项目列表、项目详情、分析运行、趋势、投入、决策、决策记录）

## 6. 门户填报信息（待补）

- 应用名称：自投研发项目重复投入治理平台
- 应用地址：待 WeCode 部署
- 代码仓库：`https://github.com/hegraphhopper/taiji`
- 数据库备份：`db/backup/fund-platform-20261101-100939.zip`
- 文档：仓库 `docs/`

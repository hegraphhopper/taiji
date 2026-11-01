# 大模型能力扩展 — 实施计划

## 顺序

- [x] V7 迁移与实体/VO 字段
- [x] 分析落库 overlaps/differences
- [x] 预检 Top-1 LLM + llmUsed 透传
- [x] 连接自检接口与前端按钮
- [x] 项目详情/决策预检展示
- [x] Word/Excel 报告模型复核段
- [x] 测试与构建

## 验证

```powershell
mvn -q test
mvn -q -DskipTests package
cd frontend; npm.cmd run typecheck; npm.cmd run build
```

- 启动 demo 后：管理员测试连接；开启 LLM 跑分析；查看项目详情；预检；导出 Word/Excel。
- 断网/错误配置时确认 `llmUsed=false` 且分数不变。

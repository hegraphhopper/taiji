# 技术创新投入态势分析 — 实施计划

## 1. 顺序清单

- [ ] `InvestmentQuery` 与 DTO；定义状态/维度枚举。
- [ ] `InvestmentQueryService`：项目与最新快照加载、筛选、内存聚合。
- [ ] KPI 与同比计算（BigDecimal、空值）。
- [ ] 四类维度分布与 Top 10 技术方向。
- [ ] `RiskExposureService`：风险敞口、Top 风险项目、高投入高重复标记。
- [ ] 散点数据接口。
- [ ] Excel/CSV 导出（POI + BOM）。
- [ ] Controller、参数校验、数据范围、OpenAPI。
- [ ] 后端单测（KPI、敞口、同比、未分析、权限、导出）。
- [ ] 前端 KPI 卡、年度/基金/组/方向图表、散点、Top 表格。
- [ ] 筛选与 URL 同步、下钻抽屉、空态/加载态。
- [ ] 页面数字与接口/数据库对账，截图留档。

## 2. 验证命令

```powershell
mvn -q clean package
mvn test -Dtest='*Investment*,*Exposure*'

java -jar target/fund-platform-1.0.0.jar --spring.profiles.active=demo

Invoke-RestMethod "http://localhost:8080/api/v1/insight/investment/overview?startYear=2025&endYear=2026" -Headers $h
Invoke-RestMethod "http://localhost:8080/api/v1/insight/investment/by-dimension?dimension=GROUP" -Headers $h
Invoke-RestMethod "http://localhost:8080/api/v1/insight/investment/risk-projects?top=10" -Headers $h
Invoke-WebRequest "http://localhost:8080/api/v1/insight/investment/export" -Headers $h -OutFile investment.xlsx

cd frontend
npm run build
```

- 对账：SQL/接口导出项目明细，复算总额、敞口、Top 项目，与页面比对。
- 权限：`user01` 验证仅本组；`expert01` 返回 403。

## 3. 风险文件与回滚

| 文件/区域 | 风险 | 回滚 |
|-----------|------|------|
| 聚合服务 | 口径错误导致指标偏差 | 单测复算 + 页面口径说明 |
| BigDecimal | 舍入不一致 | 统一 HALF_UP、2 位、单测 |
| 导出 | POI 内存/中文乱码 | SXSSF + UTF-8；限制导出行数 |
| 图表 | 大数据量渲染慢 | Top 10 + 其他合并；按需加载 |

## 4. 完成定义（DoD）

- KPI、四类分布、敞口、散点、Top 表格均可展示并与明细复算一致。
- 权限、空态、未分析标识、同比空值处理正确。
- Excel/CSV 可打开且与页面一致。
- 后端测试与 `npm run build` 通过。
- 截图与口径说明进入交付文档。

## 5. 交接给下游

- 决策页可复用风险敞口与最新快照读取逻辑，避免各自实现。
- 口径说明统一维护在 `docs/metrics.md`。

# 技术调研与决策记录

## 1. 环境事实

| 项 | 值 | 来源 |
|----|----|------|
| JDK | 1.8.0_65 | `java -version` |
| Maven | 3.5.0 | `mvn -v` |
| Node | v24.12.0 | `node --version` |
| Python | 不可用（WindowsApps 占位） | `where python.exe`、执行无输出 |
| MySQL | 未安装 | `where mysql` 为空 |
| Docker | 未安装 | `where docker` 为空 |
| 既有代码 | `com.example:helloworld:1.0.0`，Java 8，无测试依赖 | `pom.xml`、`src/main/java` |

结论：开发/演示以 H2 文件库为主路径；MySQL/Docker 脚本在 WeCode 环境验证；Trellis 脚本不可用，任务文件手工维护。

## 2. 网络可达性（2026-10-29 实测）

| 目标 | 结果 |
|------|------|
| `https://repo.maven.apache.org/maven2/...spring-boot-starter-parent-2.7.18.pom` | 200 |
| `https://raw.githubusercontent.com/apache/shardingsphere/master/README.md` | 200 |
| `https://api.github.com/search/repositories?q=项目申报` | 403（未认证限流） |

结论：Maven 依赖可直连下载；GitHub raw 可用于获取开源样例文档；GitHub 搜索 API 不可靠，样例改用已知开源仓库的固定 raw 链接。

## 3. 版本兼容性

| 依赖 | 版本 | 兼容性结论 |
|------|------|------------|
| Spring Boot | 2.7.18 | 支持 Java 8；Maven 3.5+；Spring Security 5.7 |
| MyBatis-Plus | 3.5.3.2 | 支持 Java 8 / Boot 2.x；分页插件 `MybatisPlusInterceptor` |
| MySQL Connector/J | 8.0.33 | 与 MySQL 8 匹配；`allowPublicKeyRetrieval=true` 解决 caching_sha2 |
| H2 | 2.1.214 | Boot 2.7 管理版本；`MODE=MySQL` 可兼容基础 DDL |
| Flyway | 8.5.x（Boot 管理） | 支持 MySQL 8 与 H2 2.x；避免使用 JSON/全文索引/存储过程 |
| JJWT | 0.11.5 | Java 8 可用；`api/impl/jackson` 三件套 |
| springdoc-openapi-ui | 1.6.15 | 匹配 Boot 2.7；路径 `/swagger-ui.html` |
| Apache POI | 5.2.3 | Java 8 可用；docx 读写与 xlsx 导出 |
| PDFBox | 2.0.29 | Java 8；仅文本层 PDF |
| Lombok | 1.18.30 | 支持 JDK 8；IDE 需开启注解处理 |

排除项：

- **EasyExcel**：3.x 依赖 POI 4.1.2，与 POI 5.2.3 冲突；Excel 导出直接用 POI `SXSSFWorkbook`。
- **poi-tl**：模板能力用不上，且增加版本耦合；Word 报告用 POI `XWPFDocument` 直接构建。
- **HanLP / jieba-analysis**：模型体积大、离线依赖重；改用字符 n-gram + 术语词典 + TF-IDF。
- **Flowable / Activiti**：状态机固定，引入工作流引擎成本过高。

## 4. 重复度算法调研结论

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| 字符 n-gram + TF-IDF + 余弦 | 零依赖、中文无需分词、可解释、稳定 | 语义泛化弱 | 采用为基础分 |
| 中文分词 + 词向量 | 语义更好 | 需模型文件、启动慢、离线风险 | 不采用 |
| 外部 LLM 语义评分 | 语义与解释强 | 需网络/密钥、成本与延迟 | 可选增强，权重可调，失败降级 |
| 结构化标签 Jaccard | 对技术方向/关键技术命中敏感 | 依赖标签质量 | 采用为独立维度 |

默认权重：目标 15、研制内容 35、关键技术 35、节点 5、技术标签 10；LLM 融合默认本地 70 / 大模型 30。

阈值：高 ≥80、中 ≥60、低 ≥40。依据：文本长度与中文重复表达特点，80 以上通常存在成段雷同；60–80 为需人工确认的部分重叠；40 以下噪声较多。

## 5. 开源样例数据计划

用途：演示附件上传与解析，不伪造“真实申报材料”。

候选来源（均为公开开源仓库文档，保留来源与许可证说明）：

1. Apache ShardingSphere `README.md` / 中文 README
2. Apache DolphinScheduler `README_zh_CN.md`
3. Apache SkyWalking `README.md` / `docs/`
4. Apache SeaTunnel `README.md`
5. Apache Dubbo 设计文档
6. 任一 Apache 项目 `docs/` 下的设计/架构文档

处理方式：

- 交付阶段下载 4–6 份到 `demo/attachments/`，保留原始链接与许可证。
- 以这些文档内容为基础，构造“项目目标 / 研制内容 / 关键技术 / 节点计划”四段式申报书文本（标注为演示构造），生成 docx 样例用于解析演示。
- 若网络不可用，退回为内置生成的示例文本，并在 `docs/demo-data.md` 说明。

## 6. 关键决策

| 决策 | 结论 | 依据 |
|------|------|------|
| 后端形态 | 单体 Spring Boot，按 feature 分包 | 交付确定性、部署简单 |
| 数据库 | 生产 MySQL 8 + 演示 H2 文件库 | 本机无 MySQL；评审需零依赖启动 |
| 迁移 | Flyway + 兼容 DDL | 一套脚本两环境 |
| 认证 | 本地账号 + JWT + RBAC + 数据范围 | 无统一身份对接条件 |
| 大模型 | OpenAI 兼容接口，默认关闭，权重可调，失败降级 | 用户 Q3；离线评审风险 |
| 归因 | 本组内部 / 他组在先 / 我方在先 / 同期并行 | 用户 Q4 |
| 前端 | Vue 3 + TS + Element Plus + ECharts | 数据密集型看板 |
| 报告 | POI 生成 docx/xlsx | 避免依赖冲突 |
| 样例数据 | 内置模拟 + 开源文档构造 | 用户 Q2 |

## 7. 待验证项（进入实现后闭环）

- [ ] Boot 2.7.18 在 Maven 3.5.0 + JDK 1.8.0_65 下完整构建。
- [ ] Flyway 兼容 DDL 同时通过 H2 2.1 与 MySQL 8（MySQL 需 WeCode 环境）。
- [ ] POI 5.2.3 解析 docx、PDFBox 解析中文 PDF 的编码与换行效果。
- [ ] 字符 n-gram + TF-IDF 在演示语料上的分档是否符合预期。
- [ ] LLM 兼容接口的实际响应格式与超时行为（无密钥时仅验证降级路径）。

# 开发报告

## 成果摘要

在原本只有 Trellis 配置的空仓库中建立了最小可用的 Java 8 + Maven 工程，并提供打印 `Hello, World!` 的命令行主程序；工程可干净构建、可运行，构建产物不入库。

## 已完成需求

- Maven 工程：坐标 `com.example:helloworld:1.0.0`，`packaging=jar`，源码与字节码目标均为 Java 8，使用 `maven-compiler-plugin` 3.8.1，未引入任何第三方依赖。
- 主程序：`com.example.helloworld.HelloWorld` 的 `main` 打印一行 `Hello, World!`；带多余参数时行为不变。
- 版本库卫生：`.gitignore` 覆盖 `target/` 与常见 IDE 工程文件，构建产物不会进入版本库。
- 五条验收标准（pom 配置、主类可编译、忽略规则、构建产出 jar、运行输出）均已实测通过。

## 遗留问题

- 生成的 jar 未配置 `Main-Class`，暂不支持 `java -jar` 直接运行；当前验收以 `java -cp` 方式为准，如后续需要可执行 jar 需另行配置。
- `pom.xml` 中 Java 8 的 `source/target` 在 `properties` 与插件配置中重复声明，属冗余但无害，未在本轮精简。
- 本轮按需求未编写框架化单元测试，也未扩展 `.gitignore`（未忽略 `*.class` 与 IDE Code Runner 临时文件）；执行过程中源码目录曾出现此类残留并导致构建失败，最终已清理，相关风险已记入规范。

## 验证结论

已按验收标准完成构建、运行与仓库卫生验证：`mvn clean package` 构建成功并产出 jar，运行输出为单行 `Hello, World!`，带参数时输出一致，`target/` 被正确忽略。未验证部分：未做框架化自动化测试（无测试依赖，结论为不适用），未验证 `java -jar` 启动方式。

详细验证依据见[验证报告](check.md)。

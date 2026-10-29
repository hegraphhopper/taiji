# helloworld 主程序

## Goal

在空的 `taijibei2026` 仓库中建立最小可用的 Maven 工程，并提供打印 `Hello, World!` 的 Java 主程序。

## Requirements

- 构建工具：Maven（当前环境 Maven 3.5.0）。
- JDK：Java 8（当前环境 JDK 1.8.0_65），源码与字节码均为 1.8。
- 构件坐标：`com.example:helloworld:1.0.0`，`packaging=jar`。
- 主类：`com.example.helloworld.HelloWorld`，提供 `public static void main(String[] args)`，向标准输出打印一行 `Hello, World!`。
- 不引入任何第三方运行时依赖，也不引入测试依赖（本轮不做单元测试）。
- 仓库尚无 `.gitignore`，需忽略 `target/` 与 IDE 临时文件。
- 本轮不修改 `.trellis/spec/`，不执行 git 提交（提交由用户另行指示）。

## Acceptance Criteria

- [x] `pom.xml` 存在，坐标、Java 8 编译配置、`maven-compiler-plugin` 3.8.1 正确。
- [x] `src/main/java/com/example/helloworld/HelloWorld.java` 存在且可编译。
- [x] `.gitignore` 覆盖 `target/`。
- [x] `mvn -q clean package` 成功，产出 `target/helloworld-1.0.0.jar`。
- [x] `java -cp target/classes com.example.helloworld.HelloWorld` 输出恰为 `Hello, World!`。

## Notes

- 首次构建需联网下载 Maven 插件（直连公网模式）。
- 轻量任务：保持 PRD-only，不产出 `design.md` / `implement.md`。

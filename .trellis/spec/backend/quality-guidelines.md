# 后端质量规范

- 在信任边界校验外部输入，保留服务端权限、路径、安全和数据完整性校验。
- 契约变化明确记录接口、字段、校验和错误行为，并按项目方式生成相关类型。
- 覆盖正常、边界、拒绝、失败和重复调用路径；有副作用的操作检查原子性及恢复行为。
- 运行项目已有相关测试和编译检查，不以进程启动代替真实链路验证。
- 检查 diff，保留无关改动，避免提交密钥、临时文件或构建产物。
- 首次调研补充真实验证命令与典型接口断言；规范更新内容使用中文。

## 技术栈事实（2026-10-29 实测）

- JDK：`java version "1.8.0_65"`；`JAVA_HOME=D:\JDK`，Maven 报告的 `Java home` 为 `D:\JDK\jre`。源码与字节码目标均为 1.8，禁用 Java 9+ 语法（`var`、`record`、switch 表达式等）。
- 构建：Apache Maven 3.5.0（`D:\JavaConfig\maven`）。插件版本需与 3.5.0 兼容，当前使用 `maven-compiler-plugin` 3.8.1。
- Maven 输出为 GBK，PowerShell 控制台可能乱码；判定构建结果以退出码与 `-q` 输出为准，不以乱码文本为依据。
- 本机 `python.exe` / `py` 不在 PATH（`WindowsApps` 下为 Store 占位符，退出码 9009）。调用 Trellis 脚本使用 `D:\SoftWare\Anaconda3\python.exe`。

## 验证命令

```powershell
mvn -q clean package        # 期望退出码 0，产出 target/<artifactId>-<version>.jar
java -cp target/classes com.example.helloworld.HelloWorld
```

- 当 PRD 明确禁止测试依赖时，单元用例以命令行方式执行（构建退出码、产物存在性、标准输出精确匹配、`git check-ignore -v target/`），并在报告中标注 `not_applicable` 说明无法产出框架化测试，`not_applicable` 不等于通过。

## 常见错误

### Common Mistake: 源码目录残留 `*.class` 或 IDE 临时 `.java` 导致构建失败

**Symptom**：`mvn -q clean package` 退出码 1，报 `重复: com.example.<pkg>.<Class>` 或「类 X 是公共的, 应在名为 X.java 的文件中声明」，`target/*.jar` 不产出。

**Cause**：手工 `javac` 把 `.class` 写回 `src/main/java`，或 IDE Code Runner 生成 `tempCodeRunnerFile.java`（内容与被测类相同、重复定义同一个 `public class`）。`.gitignore` 只忽略 `target/`，这些残留会以未跟踪文件形式存在，且被 Maven 一并编译。

**Fix**：删除 `src/main/java` 下的 `.class` 与 `tempCodeRunnerFile*.java`，重新构建。

**Prevention**：编译产物只允许出现在 `target/`；提交前执行 `git status --porcelain --untracked-files=all -- src` 确认源码目录只含预期源文件。若确需扩展忽略规则，需先与用户确认（当前 `.gitignore` 仅覆盖 `target/` 与 IDE 工程文件）。

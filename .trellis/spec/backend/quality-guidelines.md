# 后端质量规范（A1 实测）

## 技术栈事实

- JDK：`1.8.0_65`；源码与字节码目标 1.8，禁用 Java 9+ 语法。
- Maven：3.5.0；`maven-compiler-plugin` 3.8.1。
- Spring Boot 2.7.18、MyBatis-Plus 3.5.3.2、H2 2.1.214、Flyway 8.5.13、JJWT 0.11.5、springdoc 1.6.15。
- Maven 输出为 GBK；判定结果以退出码与 `-q` 输出为准。

## 验证命令

```powershell
mvn -q test                    # 单元测试，必须 22/22 通过
mvn -q -DskipTests package     # 构建 jar
mvn -q clean package           # 全量构建；执行前必须停止后端，避免 Windows 锁定 jar

cd frontend
npm.cmd run typecheck
npm.cmd run build
```

- 不要在后端 jar 正在运行时执行 `mvn clean/package`：Windows 会锁定 `target/fund-platform-1.0.0.jar`，导致删除/覆盖失败。
- 运行态验证使用 `scripts/start-demo.ps1`、`scripts/status-demo.ps1`、`scripts/stop-demo.ps1`；禁止在前台工具调用中直接启动服务（会持有输出管道导致调用不返回）。
- 健康检查：`http://localhost:8080/api/v1/health`；前端：`http://127.0.0.1:5173`。

## 测试规范

- 框架：JUnit 5 + Spring Boot Test + MockMvc；测试库使用 `src/test/resources/application-test.yml`（H2 内存、随机库名）。
- 测试类 `@ActiveProfiles({"demo","test"})`，由 `DemoAccountInitializer` 初始化演示账号。
- 测试数据使用唯一用户名，避免共享上下文污染；锁定/改密测试使用专用账号。
- 禁止 `skip`/`only`/删除断言；失败如实记录。
- 测试报告写入 `.trellis/tasks/<task>/test/unit/report-round-N.md`。
- 第一轮不达标进入 `trellis-test-rework`；每轮 ≤ `max_fix_rounds`。

## 代码检查

- Controller 不返回实体；Service 事务边界清晰。
- 外部输入 `@Valid`；SQL 参数绑定；排序字段白名单。
- 错误码与 HTTP 状态匹配；不泄露堆栈。
- 新增写接口必须 `@Audit`。
- 不提交密钥、Token、真实数据、构建产物。

## Common Mistake: 常驻进程阻塞工具调用

**Symptom**：启动服务后工具调用长时间不返回，健康检查已成功但会话卡住。

**Cause**：`Start-Process -NoNewWindow` 或前台命令启动的 Java/Node 子进程继承标准输出管道，工具等待管道关闭。

**Fix**：停止子进程；改用 `Start-Process -WindowStyle Hidden` + 文件重定向，或使用 `scripts/start-demo.ps1`。

**Prevention**：启动、轮询、停止拆成独立调用；不要在子代理提示中要求“前台启动服务并等待”。

# 后端目录结构

- 沿实际调用链确认代码归属，保留项目既有路由、业务和存储边界。
- 优先复用已有服务、工具函数和类型，不为单个使用点新增框架或抽象。
- 修改共享实现前检查所有实际调用方；修复放在导致问题的共同入口。
- 首次调研按真实源码补充入口、模块职责与典型调用路径，不添加不存在的架构层。

## 当前工程结构（2026-10-29）

Maven 单模块工程，坐标 `com.example:helloworld:1.0.0`：

```
pom.xml                                    # Java 8，maven-compiler-plugin 3.8.1，无 dependencies
src/main/java/com/example/helloworld/      # 唯一源码根
  HelloWorld.java                          # public class HelloWorld + main(String[])
.gitignore                                 # target/、.idea/、*.iml、.classpath、.project、.settings/
```

- 尚无 `src/test`、无 `src/main/resources`；新增目录前先确认 PRD 是否允许，当前 PRD 明确禁止测试依赖。
- 编译产物只允许在 `target/`，`.class` 不得出现在 `src/` 下。

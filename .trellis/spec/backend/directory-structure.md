# 后端目录结构（A1 实测）

## 工程结构

Maven 单模块，坐标 `com.taiji:fund-platform:1.0.0`，Java 8。

```text
pom.xml
src/main/java/com/taiji/fund/
  FundApplication.java                 启动类，Mapper 扫描
  common/
    api/                               Result、PageResult
    audit/                             @Audit 注解、AuditAspect
    enums/                             FundType、ProjectStatus、RoleCode、UserStatus
    exception/                         ErrorCode、BizException、GlobalExceptionHandler
    util/                              通用工具
  config/                              SecurityConfig、MybatisPlusConfig、MetaObjectHandler、
                                       JacksonConfig、WebConfig、OpenApiConfig
  security/                            JwtProperties、JwtTokenProvider、JwtAuthenticationFilter、
                                       LoginUser、UserDetailsServiceImpl、AuthService、AuthController、
                                       LoginAttemptService、DataScopeService、SecurityUtils、
                                       RestAuthenticationEntryPoint、RestAccessDeniedHandler
  system/
    user/                              User、UserMapper
    role/                              Role、UserRole、Mapper
    group/                             Group、Mapper、Service、Controller、VO
    techdirection/                     TechDirection、Mapper、Service、Controller、VO
    dict/                              DictController、DictService、DictVO、DictItemVO
    audit/                             AuditLog、Mapper、AuditService
  health/                              HealthController
  bootstrap/                           DemoAccountInitializer（INIT 哈希初始化）
src/main/resources/
  application.yml                      公共配置，默认 profile=demo
  application-demo.yml                 H2 文件库、Flyway、SQL 日志
  application-prod.yml                 MySQL、JWT_SECRET 必填、SQL 日志关闭
  db/migration/V1__schema.sql          系统表
  db/migration/V2__seed_core.sql       角色/项目组/技术方向/演示账号种子
src/test/java/com/taiji/fund/          JUnit 5 + MockMvc 测试
src/test/resources/application-test.yml H2 内存库、随机库名、测试 JWT
frontend/                              Vue 3 + TS 前端（见前端规范）
scripts/                               start/stop/status 脚本
docs/                                  产品设计与交付文档
```

## 分层与调用边界

- Controller：只做参数绑定、校验、调用 Service、返回 `Result`；不直接返回实体。
- Service：事务边界；写操作 `@Transactional`，只读查询 `@Transactional(readOnly = true)`。
- Mapper：MyBatis-Plus；动态条件用 `Wrappers` 参数绑定，禁止拼接 SQL。
- DTO/VO 与实体分离；用户输入必须经过 `@Valid`。
- 安全与数据范围：Controller/Service 通过 `SecurityUtils`、`DataScopeService` 获取当前用户与范围；禁止只在前端隐藏入口。
- 审计：写操作标注 `@Audit(action, resourceType)`；切面失败不阻断主流程。

## 约定

- 新功能按 feature 建包，不新建与现有层级平行的抽象。
- 共享工具先搜索 `common/util`、`SecurityUtils`、`DataScopeService`，避免重复实现。
- 测试放在 `src/test/java` 对应包，命名 `*Test`；不使用整类跳过。
- 编译产物只出现在 `target/`；`src/` 下不得残留 `.class` 或 `tempCodeRunnerFile*.java`。

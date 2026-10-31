# 数据库规范（A1 实测）

## 1. Scope / Trigger

涉及 Flyway 迁移、表结构、索引、事务或 H2/MySQL 双库兼容时必须遵守本规范。

## 2. 迁移与表结构

- 迁移目录：`src/main/resources/db/migration`，命名 `V{n}__{name}.sql`；已发布的迁移只增不改。
- 当前版本：`V1__schema.sql`（系统表）、`V2__seed_core.sql`（种子）。
- 双库：demo 使用 H2 2.1 文件库（`MODE=MySQL`），prod 使用 MySQL 8；脚本只能使用两者共同支持的语法。
- 禁止：`JSON` 列、`FULLTEXT`、`ENGINE=`、`CHARSET=`、存储过程、`ON UPDATE CURRENT_TIMESTAMP`。
- 允许：`BIGINT AUTO_INCREMENT`、`VARCHAR`、`TEXT/LONGTEXT`、`DATETIME`、`DATE`、`DECIMAL`、`TINYINT`、普通/唯一索引。
- 时间字段由应用层写入；种子脚本可用 `CURRENT_TIMESTAMP` 作为 INSERT 值。
- 逻辑删除字段 `deleted TINYINT DEFAULT 0`；唯一索引需考虑软删后不可复用（A1 无删除接口，后续若需复用改为组合唯一键或物理删除策略）。

## 3. 事务与查询

- 事务边界在 Service 层；多表写入同一事务。
- 只读查询标注 `@Transactional(readOnly = true)`。
- 登录失败计数/锁定写入必须独立事务，避免被登录异常回滚（见 `LoginAttemptService` 的 `REQUIRES_NEW`）。
- 外部网络调用（LLM、文件解析）不得持有数据库长事务。

## 4. 测试库

- 测试使用 `src/test/resources/application-test.yml`：`jdbc:h2:mem:fundtest-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=-1`。
- 测试类使用 `@ActiveProfiles({"demo","test"})`，Flyway 自动迁移；不得连接 `data/` 文件库。
- 每个测试上下文使用随机库名，避免跨测试污染；共享上下文时用唯一用户名造数。

## 5. Good / Base / Bad

- Good：新增列写新迁移，H2 与 MySQL 均可执行；索引覆盖过滤字段。
- Base：仅改种子值，修改未发布迁移或新增 `V3__...`。
- Bad：在 `V1` 中直接改已发布内容；使用 `JSON` 存储分析结果导致 H2 失败。

## 6. 测试要求

- `mvn -q test` 必须通过；Flyway 冒烟测试校验表存在与种子数量（角色 4、项目组 8、技术方向 12、用户 4）。
- 涉及迁移的改动至少验证一次全新内存库启动。

## 7. Wrong vs Correct

### Wrong

```sql
CREATE TABLE demo (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payload JSON,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Correct

```sql
CREATE TABLE demo (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payload TEXT,
  updated_at DATETIME
);
```

### 应用层写入时间

```java
entity.setUpdatedAt(LocalDateTime.now());
mapper.updateById(entity);
```

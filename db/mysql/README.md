# MySQL 8 交付说明

- `schema.sql`：由 Flyway `V1`–`V7` 生成，包含系统表、项目/申报书、分析、决策与系统配置；含核心角色/项目组/技术方向/管理员种子。
- `data.sql`：演示数据占位说明。60 个演示项目由 demo profile 首次启动时自动生成。
- 导入：

```powershell
mysql -u root -p -e "CREATE DATABASE fund_platform DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p fund_platform < db/mysql/schema.sql
mysql -u root -p fund_platform < db/mysql/data.sql
```

- 本机无 MySQL 8 实例，以上脚本未在本机实际导入验证；已按 H2/MySQL 共同语法编写，需在目标环境补验。
- 生产启动：`SPRING_PROFILES_ACTIVE=prod`，配置 `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET/ADMIN_INIT_PASSWORD`。
- H2 演示库备份见 `db/backup/`，恢复脚本见 `scripts/restore-db.ps1`。

# 数据库备份说明

- 备份文件：`fund-platform-20261101-100939.zip`（H2 文件库，约 0.22 MB）。
- 生成方式：停止应用后执行 `scripts/backup-db.ps1`，压缩 `data/`。
- 恢复：停止应用后执行 `scripts/restore-db.ps1 -Archive db/backup/<file>.zip -Force`，再启动 demo。
- 本次已验证：解压后 `funddb.mv.db` 为 1,634,304 字节，与源文件一致；未在恢复库上重复启动（避免覆盖当前演示数据）。
- 备份不含明文密码：账号密码为 BCrypt 哈希；演示账号仅用于本地演示。

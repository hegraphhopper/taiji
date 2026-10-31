package com.taiji.fund.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UT-17：Flyway 建表与种子脚本在 H2(MySQL 模式) 上可执行且幂等。
 *
 * <p>使用独立内存库（{@code DB_CLOSE_DELAY=-1} 保证同一 JVM 内连接复用），
 * 连续执行两次迁移模拟“重启两次”，验证种子不重复插入。</p>
 */
class FlywayMigrationTest {

    private static final String URL = "jdbc:h2:mem:fund-flyway-it;MODE=MySQL;DB_CLOSE_DELAY=-1;"
            + "DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";

    @Test
    @DisplayName("UT-17 Flyway 种子数量正确且二次迁移不重复插入")
    void ut17_seedDataIsCompleteAndMigrationIsIdempotent() throws Exception {
        Flyway first = newFlyway();
        MigrateResult firstResult = first.migrate();

        assertThat(firstResult.migrationsExecuted).isEqualTo(2);
        assertThat(first.info().current().getVersion().getVersion()).isEqualTo("2");
        assertSeedCounts();

        Flyway second = newFlyway();
        MigrateResult secondResult = second.migrate();

        assertThat(secondResult.migrationsExecuted).isZero();
        assertThat(second.info().current().getVersion().getVersion()).isEqualTo("2");
        assertSeedCounts();

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT username, password_hash FROM sys_user ORDER BY id")) {
            int rows = 0;
            while (rs.next()) {
                rows++;
                assertThat(rs.getString("password_hash"))
                        .as("种子密码必须为 INIT 占位，禁止明文入库")
                        .isEqualTo("INIT");
            }
            assertThat(rows).isEqualTo(4);
        }
    }

    private Flyway newFlyway() {
        return Flyway.configure()
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    private void assertSeedCounts() throws Exception {
        assertThat(count("sys_role")).isEqualTo(4);
        assertThat(count("sys_group")).isEqualTo(8);
        assertThat(count("tech_direction")).isEqualTo(12);
        assertThat(count("sys_user")).isEqualTo(4);
        assertThat(count("sys_user_role")).isEqualTo(4);
    }

    private int count(String table) throws Exception {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(URL, "sa", "");
    }
}

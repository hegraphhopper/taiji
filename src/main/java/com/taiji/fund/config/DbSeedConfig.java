package com.taiji.fund.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 首次启动时用内置数据库快照初始化数据文件。
 *
 * <p>用于把本地测试库整体带到部署环境：仅当标记文件不存在时才覆盖目标数据文件，
 * 覆盖成功后写入标记，之后的重启/重新部署都保留运行期数据。</p>
 */
@Slf4j
@Configuration
public class DbSeedConfig {

    @Value("${app.db.seed.enabled:false}")
    private boolean enabled;

    @Value("${app.db.seed.resource:classpath:db/seed/funddb.mv.db}")
    private String resource;

    @Value("${app.db.seed.target:}")
    private String target;

    @Value("${app.db.seed.marker:}")
    private String marker;

    @Bean
    public FlywayMigrationStrategy seedDatabaseThenMigrate() {
        return flyway -> {
            seedIfNeeded();
            flyway.migrate();
        };
    }

    private void seedIfNeeded() {
        if (!enabled || target == null || target.trim().isEmpty()) {
            return;
        }
        try {
            Path targetPath = Paths.get(target.trim()).toAbsolutePath();
            Path markerPath = (marker == null || marker.trim().isEmpty())
                    ? targetPath.resolveSibling(targetPath.getFileName() + ".seeded")
                    : Paths.get(marker.trim()).toAbsolutePath();
            if (Files.exists(markerPath)) {
                return;
            }
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Path tmp = targetPath.resolveSibling(targetPath.getFileName() + ".seeding");
            String location = resource.startsWith("classpath:")
                    ? resource.substring("classpath:".length())
                    : resource;
            try (InputStream in = new ClassPathResource(location).getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING);
            if (markerPath.getParent() != null) {
                Files.createDirectories(markerPath.getParent());
            }
            Files.write(markerPath, "seeded".getBytes(StandardCharsets.UTF_8));
            log.info("已用内置数据库快照初始化 {}", targetPath);
        } catch (Exception e) {
            throw new IllegalStateException("数据库快照初始化失败: " + e.getMessage(), e);
        }
    }
}

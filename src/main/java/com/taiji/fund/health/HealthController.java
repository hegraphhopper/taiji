package com.taiji.fund.health;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final Environment environment;

    @Value("${app.name:fund-platform}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String version;

    @GetMapping
    public Result<Map<String, Object>> health() {
        boolean databaseUp = checkDatabase();
        String profile = environment.getActiveProfiles().length == 0
                ? "default"
                : String.join(",", environment.getActiveProfiles());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", databaseUp ? "UP" : "DOWN");
        data.put("app", appName);
        data.put("version", version);
        data.put("profile", profile);
        data.put("db", databaseUp ? "UP" : "DOWN");
        data.put("time", LocalDateTime.now());
        return Result.ok(data);
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            log.warn("数据库连通性检查失败: {}", ex.getMessage());
            return false;
        }
    }
}

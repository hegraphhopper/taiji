package com.taiji.fund.bootstrap;

import com.taiji.fund.AbstractApiTest;
import com.taiji.fund.security.AuthService;
import com.taiji.fund.security.dto.LoginRequest;
import com.taiji.fund.security.dto.LoginResponse;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UT-15：demo 环境演示账号 INIT 占位哈希初始化与幂等。
 */
class SeedDataTest extends AbstractApiTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("UT-15 演示账号 INIT 占位被 BCrypt 替换，重复初始化不重复编码")
    void ut15_demoAccountsInitializedFromInitPlaceholder() throws Exception {
        Map<String, String> accounts = new LinkedHashMap<>();
        accounts.put("admin", "Admin@123");
        accounts.put("pmo01", "Pmo@12345");
        accounts.put("user01", "User@12345");
        accounts.put("expert01", "Expert@12345");

        for (Map.Entry<String, String> entry : accounts.entrySet()) {
            User user = findByUsername(entry.getKey());
            assertThat(user).as("种子账号 %s 应存在", entry.getKey()).isNotNull();
            assertThat(user.getPasswordHash()).isNotEqualTo("INIT");
            assertThat(user.getPasswordHash()).startsWith("$2");

            LoginResponse response = authService.login(loginRequest(entry.getKey(), entry.getValue()));
            assertThat(response.getToken()).as("演示账号 %s 应可登录", entry.getKey()).isNotBlank();
        }

        String adminHashBefore = findByUsername("admin").getPasswordHash();
        ApplicationRunner initializer = applicationContext.getBean("demoAccountInitializer", ApplicationRunner.class);
        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertThat(findByUsername("admin").getPasswordHash()).isEqualTo(adminHashBefore);
        for (String username : accounts.keySet()) {
            assertThat(findByUsername(username).getPasswordHash()).startsWith("$2");
        }
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}

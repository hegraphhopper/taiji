package com.taiji.fund.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.security.UserDetailsServiceImpl;
import com.taiji.fund.system.role.Role;
import com.taiji.fund.system.role.RoleMapper;
import com.taiji.fund.system.role.UserRole;
import com.taiji.fund.system.role.UserRoleMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoAccountInitializer implements ApplicationRunner {

    private static final String INIT_PLACEHOLDER = "INIT";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");
    private static final Map<String, String> DEMO_ACCOUNTS = new LinkedHashMap<>();

    static {
        DEMO_ACCOUNTS.put("admin", "Admin@123");
        DEMO_ACCOUNTS.put("pmo01", "Pmo@12345");
        DEMO_ACCOUNTS.put("user01", "User@12345");
        DEMO_ACCOUNTS.put("expert01", "Expert@12345");
    }

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (isDemoProfile()) {
            initializeDemoAccounts();
        } else {
            ensureAdminAccount();
        }
    }

    private boolean isDemoProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return true;
        }
        for (String profile : profiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                return false;
            }
        }
        for (String profile : profiles) {
            if ("demo".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private void initializeDemoAccounts() {
        List<String> initialized = new ArrayList<>();
        DEMO_ACCOUNTS.forEach((username, password) -> {
            User user = findByUsername(username);
            if (user != null && INIT_PLACEHOLDER.equals(user.getPasswordHash())) {
                updatePassword(user.getId(), password);
                initialized.add(username);
            }
        });
        if (!initialized.isEmpty()) {
            log.info("已初始化演示账号密码: {}", String.join(", ", initialized));
        }
    }

    private void ensureAdminAccount() {
        String initPassword = environment.getProperty("ADMIN_INIT_PASSWORD");
        User admin = findByUsername("admin");
        if (admin == null) {
            if (isBlank(initPassword)) {
                throw new IllegalStateException("生产环境缺少管理员账号，请配置 ADMIN_INIT_PASSWORD 环境变量");
            }
            validatePassword(initPassword);
            admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode(initPassword));
            admin.setRealName("系统管理员");
            admin.setStatus(UserStatus.ENABLED.name());
            admin.setFailCount(0);
            admin.setDeleted(0);
            userMapper.insert(admin);
            bindAdminRole(admin.getId());
            log.info("已创建生产管理员账号: admin");
            return;
        }
        if (INIT_PLACEHOLDER.equals(admin.getPasswordHash())) {
            if (isBlank(initPassword)) {
                throw new IllegalStateException("管理员密码未初始化，请配置 ADMIN_INIT_PASSWORD 环境变量");
            }
            validatePassword(initPassword);
            updatePassword(admin.getId(), initPassword);
            bindAdminRole(admin.getId());
            log.info("已使用 ADMIN_INIT_PASSWORD 初始化管理员账号: admin");
        }
    }

    private void bindAdminRole(Long userId) {
        List<String> roles = userDetailsService.loadRoles(userId);
        if (roles.contains(RoleCode.ADMIN.name())) {
            return;
        }
        Role adminRole = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, RoleCode.ADMIN.name()));
        if (adminRole == null) {
            throw new IllegalStateException("缺少 ADMIN 角色种子数据，无法完成管理员初始化");
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(userRole);
    }

    private void updatePassword(Long userId, String rawPassword) {
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(rawPassword))
                .set(User::getFailCount, 0)
                .set(User::getLockedUntil, null)
                .set(User::getUpdatedAt, LocalDateTime.now()));
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalStateException("ADMIN_INIT_PASSWORD 至少 8 位且需同时包含字母和数字");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

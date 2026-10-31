package com.taiji.fund;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.UserDetailsServiceImpl;
import com.taiji.fund.system.audit.AuditLogMapper;
import com.taiji.fund.system.role.Role;
import com.taiji.fund.system.role.RoleMapper;
import com.taiji.fund.system.role.UserRole;
import com.taiji.fund.system.role.UserRoleMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 单元测试公共基类：统一 Spring Boot + H2 内存库 + MockMvc 环境。
 *
 * <p>测试数据全部使用唯一用户名（{@link #uniqueUsername(String)}）独立创建，
 * 避免不同测试类共享同一 ApplicationContext 时相互污染。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"demo", "test"})
public abstract class AbstractApiTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected RoleMapper roleMapper;

    @Autowired
    protected UserRoleMapper userRoleMapper;

    @Autowired
    protected AuditLogMapper auditLogMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserDetailsServiceImpl userDetailsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(rawBody(result));
    }

    protected String rawBody(MvcResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    protected String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("token").asText();
    }

    protected String uniqueUsername(String prefix) {
        return prefix + "-" + SEQUENCE.incrementAndGet() + "-" + System.nanoTime();
    }

    protected User createUser(String prefix, String rawPassword, UserStatus status, Long groupId,
                              RoleCode role, int failCount) {
        User user = new User();
        user.setUsername(uniqueUsername(prefix));
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRealName(prefix + "用户");
        user.setStatus(status.name());
        user.setGroupId(groupId);
        user.setFailCount(failCount);
        user.setDeleted(0);
        userMapper.insert(user);
        if (role != null) {
            bindRole(user.getId(), role);
        }
        return user;
    }

    protected User createEnabledUser(String prefix, String rawPassword, RoleCode role) {
        return createUser(prefix, rawPassword, UserStatus.ENABLED, null, role, 0);
    }

    protected void bindRole(Long userId, RoleCode roleCode) {
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode.name()));
        if (role == null) {
            throw new IllegalStateException("缺少角色种子数据: " + roleCode);
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    protected User findByUsername(String username) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    protected LoginUser authenticate(String username) {
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
        return loginUser;
    }

    protected void authenticate(User user) {
        authenticate(user.getUsername());
    }
}

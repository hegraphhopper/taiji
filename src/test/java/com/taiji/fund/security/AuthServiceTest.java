package com.taiji.fund.security;

import com.taiji.fund.AbstractApiTest;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.security.dto.ChangePasswordRequest;
import com.taiji.fund.security.dto.LoginRequest;
import com.taiji.fund.security.dto.LoginResponse;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UT-01 ~ UT-04、UT-13、UT-14、UT-20：登录、锁定、禁用、改密与凭证泄露防护。
 */
class AuthServiceTest extends AbstractApiTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("UT-01 正确账号密码登录成功，签发有效 JWT 并清零失败计数")
    void ut01_loginSucceedsAndIssuesToken() {
        LoginResponse response = authService.login(loginRequest("admin", "Admin@123"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(response.getRoles()).contains(RoleCode.ADMIN.name());
        assertThat(response.getUser().getUsername()).isEqualTo("admin");
        assertThat(response.getUser().getRoles()).contains(RoleCode.ADMIN.name());
        assertThat(jwtTokenProvider.getUsername(response.getToken())).isEqualTo("admin");
        assertThat(jwtTokenProvider.getRoles(response.getToken())).contains(RoleCode.ADMIN.name());

        User admin = findByUsername("admin");
        assertThat(admin.getFailCount()).isZero();
        assertThat(admin.getLockedUntil()).isNull();
        assertThat(admin.getLastLoginAt()).isNotNull();

        User withHistory = createUser("ut01", "Passw0rd!", UserStatus.ENABLED, null, RoleCode.GROUP_USER, 3);
        authService.login(loginRequest(withHistory.getUsername(), "Passw0rd!"));
        User reloaded = userMapper.selectById(withHistory.getId());
        assertThat(reloaded.getFailCount()).isZero();
        assertThat(reloaded.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("UT-02 密码错误登录失败，返回 40101 且失败计数 +1")
    void ut02_wrongPasswordRejectedAndFailureCounted() {
        User user = createEnabledUser("ut02", "Passw0rd!", RoleCode.GROUP_USER);

        BizException ex = assertThrows(BizException.class,
                () -> authService.login(loginRequest(user.getUsername(), "wrong-password")));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS.getCode());
        assertThat(ex.getHttpStatus()).isEqualTo(401);
        assertThat(userMapper.selectById(user.getId()).getFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("UT-03 连续失败 5 次锁定账号，第 6 次正确密码仍返回 40102")
    void ut03_fiveFailuresLockAccount() {
        String rawPassword = "Passw0rd!";
        User user = createEnabledUser("ut03", rawPassword, RoleCode.GROUP_USER);

        for (int i = 1; i <= 5; i++) {
            final String wrong = "bad-password-" + i;
            BizException ex = assertThrows(BizException.class,
                    () -> authService.login(loginRequest(user.getUsername(), wrong)));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS.getCode());
        }

        User locked = userMapper.selectById(user.getId());
        assertThat(locked.getFailCount()).isEqualTo(5);
        assertThat(locked.getLockedUntil()).isNotNull();
        assertThat(locked.getLockedUntil()).isAfter(LocalDateTime.now());

        BizException lockedEx = assertThrows(BizException.class,
                () -> authService.login(loginRequest(user.getUsername(), rawPassword)));
        assertThat(lockedEx.getCode()).isEqualTo(ErrorCode.AUTH_LOCKED.getCode());
    }

    @Test
    @DisplayName("UT-04 禁用用户无法登录，且 UserDetails 标记为不可用")
    void ut04_disabledUserCannotLogin() {
        String rawPassword = "Passw0rd!";
        User disabled = createUser("ut04", rawPassword, UserStatus.DISABLED, null, RoleCode.GROUP_USER, 0);

        BizException ex = assertThrows(BizException.class,
                () -> authService.login(loginRequest(disabled.getUsername(), rawPassword)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.AUTH_DISABLED.getCode());
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(disabled.getUsername());
        assertThat(loginUser.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT-13 修改密码旧密码错误返回 40104 且哈希不变")
    void ut13_changePasswordWithWrongOldPassword() {
        User user = createEnabledUser("ut13", "OldPass@1", RoleCode.GROUP_USER);
        authenticate(user);
        String originalHash = userMapper.selectById(user.getId()).getPasswordHash();

        BizException ex = assertThrows(BizException.class,
                () -> authService.changePassword(changePasswordRequest("wrong-old-password", "NewPass@123")));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.AUTH_OLD_PASSWORD_INVALID.getCode());
        assertThat(userMapper.selectById(user.getId()).getPasswordHash()).isEqualTo(originalHash);
    }

    @Test
    @DisplayName("UT-14 新密码强度不足被拒绝且哈希不变")
    void ut14_weakNewPasswordRejected() {
        String rawPassword = "OldPass@1";
        User user = createEnabledUser("ut14", rawPassword, RoleCode.GROUP_USER);
        authenticate(user);
        String originalHash = userMapper.selectById(user.getId()).getPasswordHash();

        String[] weakPasswords = {"123456", "abcdefgh"};
        for (String weak : weakPasswords) {
            BizException ex = assertThrows(BizException.class,
                    () -> authService.changePassword(changePasswordRequest(rawPassword, weak)));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PASSWORD_WEAK.getCode());
            assertThat(ex.getMessage()).contains("8");
            assertThat(userMapper.selectById(user.getId()).getPasswordHash()).isEqualTo(originalHash);
        }
    }

    @Test
    @DisplayName("UT-20 登录失败不泄露账号是否存在")
    void ut20_failureMessageDoesNotRevealAccountExistence() {
        BizException existing = assertThrows(BizException.class,
                () -> authService.login(loginRequest("admin", "definitely-wrong-password")));
        BizException missing = assertThrows(BizException.class,
                () -> authService.login(loginRequest(uniqueUsername("ut20-nobody"), "any-password")));

        assertThat(existing.getCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS.getCode());
        assertThat(missing.getCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS.getCode());
        assertThat(existing.getMessage()).isEqualTo(missing.getMessage());
        assertThat(missing.getMessage()).doesNotContain("不存在").doesNotContain("未找到");
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private ChangePasswordRequest changePasswordRequest(String oldPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        return request;
    }
}

package com.taiji.fund.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.security.dto.ChangePasswordRequest;
import com.taiji.fund.security.dto.LoginRequest;
import com.taiji.fund.security.dto.LoginResponse;
import com.taiji.fund.security.dto.UserInfoVO;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.AUTH_DISABLED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long remaining = Math.max(1L, Duration.between(now, user.getLockedUntil()).toMinutes() + 1L);
            throw new BizException(ErrorCode.AUTH_LOCKED, "账号已锁定，请 " + remaining + " 分钟后重试");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(user, now);
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        loginAttemptService.clearFailure(user.getId(), now);

        List<String> roles = userDetailsService.loadRoles(user.getId());
        String groupName = resolveGroupName(user.getGroupId());
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getRealName(), user.getGroupId(), groupName, roles, true);
        JwtTokenProvider.GeneratedToken generatedToken = tokenProvider.createToken(loginUser);

        user.setLastLoginAt(now);
        UserInfoVO userInfo = buildUserInfo(user, roles, groupName);
        return new LoginResponse(generatedToken.getToken(), generatedToken.getExpiresAt(),
                userInfo, roles, buildPermissions(roles));
    }

    @Transactional(readOnly = true)
    public UserInfoVO currentUser() {
        LoginUser loginUser = SecurityUtils.currentUser();
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        List<String> roles = userDetailsService.loadRoles(user.getId());
        return buildUserInfo(user, roles, resolveGroupName(user.getGroupId()));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        LoginUser loginUser = SecurityUtils.currentUser();
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.AUTH_OLD_PASSWORD_INVALID);
        }
        if (!PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            throw new BizException(ErrorCode.PASSWORD_WEAK);
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getPasswordHash, passwordEncoder.encode(request.getNewPassword()))
                .set(User::getUpdatedAt, LocalDateTime.now()));
    }

    private String resolveGroupName(Long groupId) {
        if (groupId == null) {
            return null;
        }
        Group group = groupMapper.selectById(groupId);
        return group == null ? null : group.getName();
    }

    private UserInfoVO buildUserInfo(User user, List<String> roles, String groupName) {
        return new UserInfoVO(user.getId(), user.getUsername(), user.getRealName(), user.getEmail(),
                user.getPhone(), user.getGroupId(), groupName, roles, user.getStatus(), user.getLastLoginAt());
    }

    private List<String> buildPermissions(List<String> roles) {
        List<String> permissions = new ArrayList<>();
        permissions.add("dashboard");
        if (roles.contains(RoleCode.ADMIN.name())) {
            permissions.add("system:manage");
        }
        if (roles.contains(RoleCode.PMO.name())) {
            permissions.add("analysis:read");
        }
        return Collections.unmodifiableList(permissions);
    }
}

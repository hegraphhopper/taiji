package com.taiji.fund.system.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.security.SecurityUtils;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.role.Role;
import com.taiji.fund.system.role.RoleMapper;
import com.taiji.fund.system.role.UserRole;
import com.taiji.fund.system.role.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户管理：分页检索、新增/编辑、启停、重置密码、角色分配。禁用由 JWT 过滤器即时生效。
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    private static final long MAX_PAGE_SIZE = 100L;
    private static final char[] TEMP_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".toCharArray();

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final GroupMapper groupMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public PageResult<UserAdminVO> list(UserAdminQuery query) {
        UserAdminQuery actual = query == null ? new UserAdminQuery() : query;
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(actual.getKeyword())) {
            String keyword = actual.getKeyword().trim();
            wrapper.and(condition -> condition.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword));
        }
        if (actual.getGroupId() != null) {
            wrapper.eq(User::getGroupId, actual.getGroupId());
        }
        if (StringUtils.hasText(actual.getStatus())) {
            wrapper.eq(User::getStatus, parseStatus(actual.getStatus()).name());
        }
        if (StringUtils.hasText(actual.getRole())) {
            List<Long> userIds = userIdsOfRole(actual.getRole().trim().toUpperCase(Locale.ROOT));
            if (userIds.isEmpty()) {
                long pageNo = Math.max(1L, actual.getPage() == null ? 1L : actual.getPage());
                long pageSize = Math.min(MAX_PAGE_SIZE,
                        Math.max(1L, actual.getSize() == null ? 10L : actual.getSize()));
                return PageResult.empty(pageNo, pageSize);
            }
            wrapper.in(User::getId, userIds);
        }
        wrapper.orderByAsc(User::getId);

        long pageNo = Math.max(1L, actual.getPage() == null ? 1L : actual.getPage());
        long pageSize = Math.min(MAX_PAGE_SIZE,
                Math.max(1L, actual.getSize() == null ? 10L : actual.getSize()));
        Page<User> page = userMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, List<String>> roleMap = roleMap(page.getRecords());
        Map<Long, String> groupNames = groupNames(page.getRecords());
        List<UserAdminVO> records = page.getRecords().stream()
                .map(user -> toVO(user, roleMap.getOrDefault(user.getId(), Collections.emptyList()),
                        groupNames.get(user.getGroupId())))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Audit(action = "SYSTEM_USER_CREATE", resourceType = "SYS_USER")
    @Transactional
    public UserAdminVO create(UserSaveRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户信息不能为空");
        }
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名需为 3-50 位字母、数字或 _.-");
        }
        if (findByUsername(username) != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
        validatePassword(request.getPassword());
        List<String> roles = normalizeRoles(request.getRoles());
        validateGroup(request.getGroupId());

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(trim(request.getRealName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));
        user.setGroupId(request.getGroupId());
        user.setStatus(parseStatus(request.getStatus()).name());
        user.setFailCount(0);
        user.setDeleted(0);
        userMapper.insert(user);
        bindRoles(user.getId(), roles);
        return toVO(user, roles, groupName(user.getGroupId()));
    }

    @Audit(action = "SYSTEM_USER_UPDATE", resourceType = "SYS_USER")
    @Transactional
    public UserAdminVO update(Long id, UserSaveRequest request) {
        User user = require(id);
        validateGroup(request.getGroupId());
        String status = parseStatus(request.getStatus()).name();
        if (id.equals(SecurityUtils.currentUserId())
                && UserStatus.DISABLED.name().equals(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能禁用当前登录账号");
        }
        user.setRealName(trim(request.getRealName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));
        user.setGroupId(request.getGroupId());
        user.setStatus(status);
        if (StringUtils.hasText(request.getPassword())) {
            validatePassword(request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setFailCount(0);
            user.setLockedUntil(null);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getRealName, user.getRealName())
                .set(User::getEmail, user.getEmail())
                .set(User::getPhone, user.getPhone())
                .set(User::getGroupId, user.getGroupId())
                .set(User::getStatus, user.getStatus())
                .set(StringUtils.hasText(request.getPassword()), User::getPasswordHash,
                        user.getPasswordHash())
                .set(StringUtils.hasText(request.getPassword()), User::getFailCount, 0)
                .set(StringUtils.hasText(request.getPassword()), User::getLockedUntil, null)
                .set(User::getUpdatedAt, user.getUpdatedAt()));
        List<String> roles = null;
        if (request.getRoles() != null) {
            roles = normalizeRoles(request.getRoles());
            bindRoles(user.getId(), roles);
        }
        if (roles == null) {
            roles = rolesOf(user.getId());
        }
        return toVO(user, roles, groupName(user.getGroupId()));
    }

    @Audit(action = "SYSTEM_USER_RESET_PASSWORD", resourceType = "SYS_USER")
    @Transactional
    public PasswordResetVO resetPassword(Long id) {
        User user = require(id);
        String temporary = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setFailCount(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return new PasswordResetVO(user.getId(), user.getUsername(), temporary);
    }

    @Audit(action = "SYSTEM_USER_ROLES", resourceType = "SYS_USER")
    @Transactional
    public UserAdminVO updateRoles(Long id, List<String> roles) {
        User user = require(id);
        List<String> normalized = normalizeRoles(roles);
        bindRoles(user.getId(), normalized);
        return toVO(user, normalized, groupName(user.getGroupId()));
    }

    private User require(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BizException(ErrorCode.PASSWORD_WEAK);
        }
    }

    private void validateGroup(Long groupId) {
        if (groupId != null && groupMapper.selectById(groupId) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目组不存在");
        }
    }

    private UserStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return UserStatus.ENABLED;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        for (UserStatus value : UserStatus.values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "用户状态必须为 ENABLED/DISABLED");
    }

    private List<String> normalizeRoles(List<String> roles) {
        Set<String> valid = new java.util.HashSet<>();
        for (RoleCode code : RoleCode.values()) {
            valid.add(code.name());
        }
        List<String> result = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                if (role == null) {
                    continue;
                }
                String normalized = role.trim().toUpperCase(Locale.ROOT);
                if (!valid.contains(normalized)) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "角色不存在：" + role);
                }
                if (!result.contains(normalized)) {
                    result.add(normalized);
                }
            }
        }
        return result;
    }

    private void bindRoles(Long userId, List<String> roles) {
        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId));
        for (String roleCode : roles) {
            Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
            if (role == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "缺少角色种子数据：" + roleCode);
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }

    private List<Long> userIdsOfRole(String roleCode) {
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectList(Wrappers.<UserRole>lambdaQuery()
                        .eq(UserRole::getRoleId, role.getId()))
                .stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
    }

    private List<String> rolesOf(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<UserRole>lambdaQuery()
                        .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream().map(Role::getCode)
                .collect(Collectors.toList());
    }

    private Map<Long, List<String>> roleMap(List<User> users) {
        Map<Long, List<String>> result = new HashMap<>();
        if (users.isEmpty()) {
            return result;
        }
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<UserRole> links = userRoleMapper.selectList(Wrappers.<UserRole>lambdaQuery()
                .in(UserRole::getUserId, userIds));
        if (links.isEmpty()) {
            return result;
        }
        Map<Long, String> roleCodes = roleMapper.selectBatchIds(
                        links.stream().map(UserRole::getRoleId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Role::getId, Role::getCode));
        for (UserRole link : links) {
            String code = roleCodes.get(link.getRoleId());
            if (code != null) {
                result.computeIfAbsent(link.getUserId(), key -> new ArrayList<>()).add(code);
            }
        }
        return result;
    }

    private Map<Long, String> groupNames(List<User> users) {
        List<Long> groupIds = users.stream().map(User::getGroupId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return groupMapper.selectList(Wrappers.<Group>lambdaQuery().in(Group::getId, groupIds))
                .stream().collect(Collectors.toMap(Group::getId, Group::getName, (l, r) -> l));
    }

    private String groupName(Long groupId) {
        if (groupId == null) {
            return null;
        }
        Group group = groupMapper.selectById(groupId);
        return group == null ? null : group.getName();
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder("Temp@");
        for (int index = 0; index < 6; index++) {
            builder.append(TEMP_CHARS[random.nextInt(TEMP_CHARS.length)]);
        }
        return builder.toString();
    }

    private UserAdminVO toVO(User user, List<String> roles, String groupName) {
        return UserAdminVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .groupId(user.getGroupId())
                .groupName(groupName)
                .status(user.getStatus())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

package com.taiji.fund.security;

import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.role.Role;
import com.taiji.fund.system.role.RoleMapper;
import com.taiji.fund.system.role.UserRole;
import com.taiji.fund.system.role.UserRoleMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final GroupMapper groupMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        List<String> roles = loadRoles(user.getId());
        String groupName = null;
        if (user.getGroupId() != null) {
            Group group = groupMapper.selectById(user.getGroupId());
            if (group != null) {
                groupName = group.getName();
            }
        }
        boolean enabled = UserStatus.ENABLED.name().equals(user.getStatus());
        return new LoginUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRealName(),
                user.getGroupId(), groupName, roles, enabled);
    }

    @Transactional(readOnly = true)
    public List<String> loadRoles(Long userId) {
        List<Long> roleIds = userRoleMapper
                .selectList(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getCode)
                .collect(Collectors.toList());
    }
}

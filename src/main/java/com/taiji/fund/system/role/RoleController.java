package com.taiji.fund.system.role;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.api.Result;
import com.taiji.fund.security.PermissionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色与权限点：固定四类角色，权限点来自 PermissionCatalog。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleMapper roleMapper;

    @GetMapping
    public Result<List<RolePermissionVO>> list() {
        List<RolePermissionVO> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                        .orderByAsc(Role::getId))
                .stream()
                .map(role -> RolePermissionVO.builder()
                        .id(role.getId())
                        .code(role.getCode())
                        .name(role.getName())
                        .description(role.getDescription())
                        .permissions(PermissionCatalog.forRole(role.getCode()))
                        .build())
                .collect(Collectors.toList());
        return Result.ok(roles);
    }
}

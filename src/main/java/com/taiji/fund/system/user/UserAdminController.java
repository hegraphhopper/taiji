package com.taiji.fund.system.user;

import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户管理：仅管理员。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public Result<PageResult<UserAdminVO>> list(UserAdminQuery query) {
        return Result.ok(userAdminService.list(query));
    }

    @PostMapping
    public Result<UserAdminVO> create(@Valid @RequestBody UserSaveRequest request) {
        return Result.ok(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserAdminVO> update(@PathVariable Long id,
                                      @Valid @RequestBody UserSaveRequest request) {
        return Result.ok(userAdminService.update(id, request));
    }

    @PostMapping("/{id}/reset-password")
    public Result<PasswordResetVO> resetPassword(@PathVariable Long id) {
        return Result.ok(userAdminService.resetPassword(id));
    }

    @PutMapping("/{id}/roles")
    public Result<UserAdminVO> updateRoles(@PathVariable Long id,
                                           @Valid @RequestBody UserRolesRequest request) {
        return Result.ok(userAdminService.updateRoles(id, request.getRoles()));
    }
}

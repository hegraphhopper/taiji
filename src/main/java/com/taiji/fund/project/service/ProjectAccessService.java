package com.taiji.fund.project.service;

import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.security.DataScopeService;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final DataScopeService dataScopeService;

    public LoginUser currentUser() {
        return SecurityUtils.currentUser();
    }

    public Long currentUserId() {
        return SecurityUtils.currentUserId();
    }

    public boolean isUnrestricted() {
        LoginUser user = SecurityUtils.currentUserOrNull();
        return user != null && (user.getRoles().contains(RoleCode.ADMIN.name())
                || user.getRoles().contains(RoleCode.PMO.name()));
    }

    public boolean isExpert() {
        LoginUser user = SecurityUtils.currentUserOrNull();
        return user != null && user.getRoles().contains(RoleCode.EXPERT.name());
    }

    public void assertCanCreate(Long groupId) {
        if (isUnrestricted()) {
            return;
        }
        LoginUser user = currentUser();
        if (user.getRoles().contains(RoleCode.GROUP_USER.name())
                && groupId != null && groupId.equals(user.getGroupId())) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "只能为本项目组创建项目");
    }

    public void assertCanView(Project project) {
        if (isUnrestricted()) {
            return;
        }
        LoginUser user = currentUser();
        if (user.getRoles().contains(RoleCode.GROUP_USER.name())
                && project.getGroupId() != null && project.getGroupId().equals(user.getGroupId())) {
            return;
        }
        if (user.getRoles().contains(RoleCode.EXPERT.name())
                && user.getGroupId() != null && user.getGroupId().equals(project.getGroupId())
                && !ProjectStatus.DRAFT.getCode().equals(project.getStatus())) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权访问该项目");
    }

    public void assertCanEdit(Project project) {
        if (isUnrestricted()) {
            return;
        }
        LoginUser user = currentUser();
        if (user.getRoles().contains(RoleCode.GROUP_USER.name())
                && project.getGroupId() != null && project.getGroupId().equals(user.getGroupId())) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权修改该项目");
    }

    public void assertGroupVisible(Long groupId) {
        if (groupId == null || isUnrestricted()) {
            return;
        }
        if (!dataScopeService.canAccessGroup(groupId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }
    }
}

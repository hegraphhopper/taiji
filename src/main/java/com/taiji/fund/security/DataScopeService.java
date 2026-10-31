package com.taiji.fund.security;

import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataScopeService {

    public boolean isUnrestricted() {
        LoginUser user = SecurityUtils.currentUserOrNull();
        if (user == null) {
            return false;
        }
        return user.getRoles().contains(RoleCode.ADMIN.name())
                || user.getRoles().contains(RoleCode.PMO.name());
    }

    public Long currentGroupId() {
        LoginUser user = SecurityUtils.currentUserOrNull();
        return user == null ? null : user.getGroupId();
    }

    public boolean canAccessGroup(Long groupId) {
        if (isUnrestricted()) {
            return true;
        }
        Long currentGroupId = currentGroupId();
        return currentGroupId != null && currentGroupId.equals(groupId);
    }

    public void assertGroupAccess(Long groupId) {
        if (!canAccessGroup(groupId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}

package com.taiji.fund.insight.trend;

import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.insight.trend.dto.TrendQuery;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import lombok.Getter;

/**
 * 趋势看板数据范围：PMO/ADMIN 全量，GROUP_USER 强制本组，EXPERT 拒绝。
 */
@Getter
public class TrendScope {

    private final boolean unrestricted;

    private final Long groupFilter;

    private TrendScope(boolean unrestricted, Long groupFilter) {
        this.unrestricted = unrestricted;
        this.groupFilter = groupFilter;
    }

    public static TrendScope resolve(TrendQuery query) {
        LoginUser user = SecurityUtils.currentUser();
        if (user.getRoles().contains(RoleCode.ADMIN.name())
                || user.getRoles().contains(RoleCode.PMO.name())) {
            return new TrendScope(true, query == null ? null : query.getGroupId());
        }
        if (user.getRoles().contains(RoleCode.GROUP_USER.name())) {
            if (user.getGroupId() == null) {
                throw new BizException(ErrorCode.FORBIDDEN, "当前账号未归属项目组");
            }
            return new TrendScope(false, user.getGroupId());
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权访问趋势看板");
    }
}

package com.taiji.fund.security;

import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UT-18：数据范围钩子（A1 仅验证规则，不接入业务查询）。
 *
 * <p>实现契约：{@code ADMIN}/{@code PMO} 不受限；其他角色仅可访问本组；
 * {@code EXPERT} 不属于不受限角色（与 design.md 括注存在差异，以用例设计与实现为准，
 * 差异已记入测试报告）。</p>
 */
class DataScopeServiceTest {

    private static final Long GROUP_A = 1L;
    private static final Long GROUP_B = 2L;

    private final DataScopeService dataScopeService = new DataScopeService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("UT-18 GROUP_USER 同组通过、跨组拒绝")
    void ut18_groupUserRestrictedToOwnGroup() {
        authenticateAsRole(GROUP_A, RoleCode.GROUP_USER);

        assertThatCode(() -> dataScopeService.assertGroupAccess(GROUP_A)).doesNotThrowAnyException();
        assertThat(dataScopeService.canAccessGroup(GROUP_A)).isTrue();
        assertThat(dataScopeService.currentGroupId()).isEqualTo(GROUP_A);
        assertThat(dataScopeService.isUnrestricted()).isFalse();

        BizException ex = assertThrows(BizException.class, () -> dataScopeService.assertGroupAccess(GROUP_B));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
        assertThat(dataScopeService.canAccessGroup(GROUP_B)).isFalse();
    }

    @Test
    @DisplayName("UT-18 ADMIN/PMO 不受数据范围限制")
    void ut18_adminAndPmoAreUnrestricted() {
        authenticateAsRole(GROUP_A, RoleCode.ADMIN);
        assertThat(dataScopeService.isUnrestricted()).isTrue();
        assertThatCode(() -> dataScopeService.assertGroupAccess(GROUP_B)).doesNotThrowAnyException();

        authenticateAsRole(GROUP_A, RoleCode.PMO);
        assertThat(dataScopeService.isUnrestricted()).isTrue();
        assertThat(dataScopeService.canAccessGroup(GROUP_B)).isTrue();
    }

    @Test
    @DisplayName("UT-18 未认证与 EXPERT 角色不做放行")
    void ut18_anonymousAndExpertAreRestricted() {
        SecurityContextHolder.clearContext();
        assertThat(dataScopeService.isUnrestricted()).isFalse();
        assertThat(dataScopeService.currentGroupId()).isNull();
        BizException anonymousEx = assertThrows(BizException.class,
                () -> dataScopeService.assertGroupAccess(GROUP_A));
        assertThat(anonymousEx.getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());

        authenticateAsRole(null, RoleCode.EXPERT);
        assertThat(dataScopeService.isUnrestricted()).isFalse();
        assertThat(dataScopeService.canAccessGroup(GROUP_A)).isFalse();
        assertThat(assertThrows(BizException.class, () -> dataScopeService.assertGroupAccess(GROUP_A)).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    private void authenticateAsRole(Long groupId, RoleCode role) {
        LoginUser loginUser = new LoginUser(101L, "ut18-user", "hash", "数据范围用户", groupId,
                groupId == null ? null : "项目组-" + groupId,
                Collections.singletonList(role.name()), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }
}

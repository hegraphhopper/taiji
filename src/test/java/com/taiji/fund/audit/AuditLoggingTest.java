package com.taiji.fund.audit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.AbstractApiTest;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.system.audit.AuditLog;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-12：登录/登出审计写入与敏感字段脱敏。
 */
class AuditLoggingTest extends AbstractApiTest {

    @Test
    @DisplayName("UT-12 登录与登出写审计，密码/凭证被脱敏")
    void ut12_loginAndLogoutAreAuditedWithMaskedSecrets() throws Exception {
        String rawPassword = "Audit@12345";
        User user = createUser("ut12", rawPassword, UserStatus.ENABLED, null, RoleCode.GROUP_USER, 0);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(user.getUsername(), rawPassword)))
                .andExpect(status().isOk());

        List<AuditLog> loginLogs = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getUsername, user.getUsername())
                .eq(AuditLog::getAction, "LOGIN")
                .orderByAsc(AuditLog::getId));
        assertThat(loginLogs).isNotEmpty();
        AuditLog loginLog = loginLogs.get(loginLogs.size() - 1);
        assertThat(loginLog.getResourceType()).isEqualTo("AUTH");
        assertThat(loginLog.getResult()).isEqualTo("SUCCESS");
        assertThat(loginLog.getIp()).isNotBlank();
        assertThat(loginLog.getCostMs()).isNotNull();
        assertThat(loginLog.getDetail()).contains("\"password\":\"***\"");
        assertThat(loginLog.getDetail()).doesNotContain(rawPassword);

        String token = loginToken(user.getUsername(), rawPassword);
        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(logoutResult).path("code").asInt()).isZero();

        List<AuditLog> logoutLogs = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getUsername, user.getUsername())
                .eq(AuditLog::getAction, "LOGOUT")
                .orderByAsc(AuditLog::getId));
        assertThat(logoutLogs).isNotEmpty();
        AuditLog logoutLog = logoutLogs.get(logoutLogs.size() - 1);
        assertThat(logoutLog.getUserId()).isEqualTo(user.getId());
        assertThat(logoutLog.getResult()).isEqualTo("SUCCESS");
        assertThat(logoutLog.getIp()).isNotBlank();
        assertThat(logoutLog.getDetail()).doesNotContain(token).doesNotContain(rawPassword);
    }
}

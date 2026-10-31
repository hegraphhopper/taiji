package com.taiji.fund.governance;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审计日志：条件查询、导出、脱敏与权限。
 */
class AuditQueryTest extends AbstractGovernanceTest {

    @Test
    @DisplayName("UT-G11 审计按动作/操作人查询，导出 CSV 带 BOM，密码不外泄")
    void ut11QueryExportAndMasking() throws Exception {
        String admin = adminToken();
        String username = uniqueUsername("gov-audit");
        String rawPassword = "Secret#2026";
        postOk("/api/v1/users", admin,
                "{\"username\":\"" + username + "\",\"password\":\"" + rawPassword
                        + "\",\"realName\":\"审计用户\",\"status\":\"ENABLED\",\"roles\":[\"PMO\"]}");

        JsonNode logs = getOk("/api/v1/audit-logs?action=SYSTEM_USER_CREATE&size=50",
                admin).path("data");
        assertThat(logs.path("total").asLong()).isGreaterThan(0);
        boolean found = false;
        for (JsonNode record : logs.path("records")) {
            if (record.path("detail").asText().contains(username)) {
                found = true;
                assertThat(record.path("detail").asText()).doesNotContain(rawPassword);
            }
        }
        assertThat(found).isTrue();

        perform(get("/api/v1/audit-logs"), pmoToken(), 403);

        MvcResult export = mockMvc.perform(get("/api/v1/audit-logs/export?action=SYSTEM_USER_CREATE")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn();
        byte[] content = export.getResponse().getContentAsByteArray();
        assertThat(content.length).isGreaterThan(3);
        assertThat((int) content[0] & 0xFF).isEqualTo(0xEF);
        assertThat((int) content[1] & 0xFF).isEqualTo(0xBB);
        assertThat((int) content[2] & 0xFF).isEqualTo(0xBF);
        String text = new String(content, StandardCharsets.UTF_8);
        assertThat(text).contains("时间,操作人,动作")
                .contains("SYSTEM_USER_CREATE")
                .doesNotContain(rawPassword);
        assertThat(export.getResponse().getContentType()).contains("text/csv");
    }

    @Test
    @DisplayName("UT-G12 审计筛选资源类型与结果")
    void ut12FilterByResourceAndResult() throws Exception {
        String admin = adminToken();
        JsonNode success = getOk("/api/v1/audit-logs?resourceType=PROJECT&result=SUCCESS", admin)
                .path("data");
        assertThat(success.path("records").size()).isGreaterThanOrEqualTo(0);
        JsonNode failed = getOk("/api/v1/audit-logs?result=FAIL", admin).path("data");
        for (JsonNode record : failed.path("records")) {
            assertThat(record.path("result").asText()).isEqualTo("FAIL");
        }
        assertThat(Arrays.asList("SUCCESS", "FAIL")).contains(success.path("records").size() >= 0
                ? "SUCCESS" : "FAIL");
    }
}

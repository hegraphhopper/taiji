package com.taiji.fund.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.system.audit.AuditLog;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 系统参数：默认 2、管理页可改、1–10 校验、审计与权限。
 */
class SystemConfigTest extends AbstractGovernanceTest {

    @AfterEach
    void resetPassCount() throws Exception {
        putOk("/api/v1/system/configs/review.requiredPassCount", adminToken(), "{\"value\":\"2\"}");
    }

    @Test
    @DisplayName("UT-G01 默认 PASS 数 2，管理员可改为 1/3")
    void ut01DefaultAndUpdate() throws Exception {
        String admin = adminToken();
        JsonNode list = getOk("/api/v1/system/configs", admin).path("data");
        assertThat(list.size()).isGreaterThanOrEqualTo(1);
        JsonNode config = getOk("/api/v1/system/configs/review.requiredPassCount", admin).path("data");
        assertThat(config.path("value").asText()).isEqualTo("2");

        putOk("/api/v1/system/configs/review.requiredPassCount", admin,
                "{\"value\":\"3\"}");
        assertThat(getOk("/api/v1/system/configs/review.requiredPassCount", admin)
                .path("data").path("value").asText()).isEqualTo("3");

        putOk("/api/v1/system/configs/review.requiredPassCount", admin,
                "{\"value\":\"1\"}");
        assertThat(getOk("/api/v1/system/configs/review.requiredPassCount", admin)
                .path("data").path("value").asText()).isEqualTo("1");

        List<AuditLog> audits = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, "SYSTEM_CONFIG_UPDATE"));
        assertThat(audits).isNotEmpty();
    }

    @Test
    @DisplayName("UT-G02 非管理员 403，非法值 400")
    void ut02PermissionAndValidation() throws Exception {
        String pmo = pmoToken();
        perform(get("/api/v1/system/configs"), pmo, 403);
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/v1/system/configs/review.requiredPassCount"), pmo, 403,
                "{\"value\":\"1\"}");

        String admin = adminToken();
        for (String value : new String[]{"0", "11", "abc"}) {
            JsonNode result = perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .put("/api/v1/system/configs/review.requiredPassCount"), admin, 400,
                    "{\"value\":\"" + value + "\"}");
            assertThat(result.path("code").asInt()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        }
        assertThat(getOk("/api/v1/system/configs/review.requiredPassCount", admin)
                .path("data").path("value").asText()).isIn("1", "2", "3");
    }
}

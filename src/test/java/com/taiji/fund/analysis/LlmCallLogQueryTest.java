package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.llm.LlmCallLog;
import com.taiji.fund.analysis.llm.LlmCallLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-L08 模型调用日志：分页、状态筛选、统计与权限。
 */
class LlmCallLogQueryTest extends AbstractAnalysisTest {

    @Autowired
    private LlmCallLogMapper callLogMapper;

    @Test
    @DisplayName("UT-L08 模型调用日志分页筛选与权限")
    void ut08LlmLogPage() throws Exception {
        insert("SUCCESS", 120L, 10, 5);
        insert("TIMEOUT", 8000L, 0, 0);

        JsonNode admin = readJson(mockMvc.perform(get("/api/v1/analysis/llm-logs")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk()).andReturn()).path("data");
        assertThat(admin.path("total").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(admin.path("records").isArray()).isTrue();
        assertThat(admin.path("successCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(admin.path("failureCount").asLong()).isGreaterThanOrEqualTo(1);

        JsonNode filtered = readJson(mockMvc.perform(get("/api/v1/analysis/llm-logs")
                        .param("status", "SUCCESS")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk()).andReturn()).path("data");
        assertThat(filtered.path("records").size()).isGreaterThanOrEqualTo(1);
        assertThat(filtered.path("records").get(0).path("status").asText()).isEqualTo("SUCCESS");

        mockMvc.perform(get("/api/v1/analysis/llm-logs")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/analysis/llm-logs")
                        .header("Authorization", bearer(loginToken("user01", "User@12345"))))
                .andExpect(status().isForbidden());
    }

    private void insert(String status, long latency, int prompt, int completion) {
        LlmCallLog log = new LlmCallLog();
        log.setModel("test-model");
        log.setStatus(status);
        log.setLatencyMs(latency);
        log.setPromptTokens(prompt);
        log.setCompletionTokens(completion);
        callLogMapper.insert(log);
    }
}

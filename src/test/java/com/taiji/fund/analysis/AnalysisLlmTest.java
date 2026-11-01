package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.llm.LlmCallResult;
import com.taiji.fund.analysis.llm.LlmClient;
import com.taiji.fund.analysis.llm.LlmResponse;
import com.taiji.fund.analysis.llm.LlmStatus;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.common.enums.ProjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestPropertySource(properties = {
        "app.llm.enabled=true",
        "app.llm.base-url=http://127.0.0.1:18080/v1",
        "app.llm.model=test-model"
})
class AnalysisLlmTest extends AbstractAnalysisTest {

    @SpyBean
    private LlmClient llmClient;

    @Test
    @DisplayName("UT-20 合法大模型响应按权重融合，100/0 时等于本地分")
    void ut20FusesLlmScore() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        LocalDateTime submitTime = LocalDateTime.of(2026, 8, 1, 9, 0);
        long projectA = buildProject(groupA, "融合项目A", submitTime);
        buildProject(groupA, "融合项目B", submitTime);

        String admin = adminToken();
        saveConfig(admin, 70, 30);

        doReturn(LlmCallResult.success(new LlmResponse(90D, Arrays.asList("重叠点"),
                        Arrays.asList("差异点"), "EXPLAIN", "语义高度相似"), 12L, 10, 5))
                .when(llmClient).call(any(), any(), any());

        JsonNode firstRun = triggerRunData(admin, "{\"triggerType\":\"FULL\"}").path("data");
        assertThat(firstRun.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(firstRun.path("llmUsed").asBoolean()).isTrue();
        List<AnalysisPair> pairs = pairsOfRun(firstRun.path("id").asLong());
        assertThat(pairs).isNotEmpty();
        assertThat(pairs).allSatisfy(pair -> {
            assertThat(pair.getLlmScore()).isEqualByComparingTo("90.00");
            assertThat(pair.getTotalScore()).isEqualByComparingTo("97.00");
            assertThat(pair.getLlmReason()).isEqualTo("语义高度相似");
        });

        saveConfig(admin, 100, 0);
        JsonNode secondRun = triggerRunData(admin, "{\"triggerType\":\"FULL\"}").path("data");
        assertThat(secondRun.path("status").asText()).isEqualTo("SUCCESS");
        List<AnalysisPair> secondPairs = pairsOfRun(secondRun.path("id").asLong());
        assertThat(secondPairs).isNotEmpty();
        assertThat(secondPairs).allSatisfy(pair -> {
            assertThat(pair.getTotalScore()).isEqualByComparingTo(pair.getLocalScore());
            assertThat(pair.getLocalScore()).isEqualByComparingTo("100.00");
        });
        assertThat(secondPairs.get(0).getProjectId()).isNotNull();
        assertThat(projectMapper.selectById(projectA).getDuplicateScore())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("UT-L04 预检 Top-1 使用大模型解释且总分不变")
    void ut21PrecheckUsesLlm() throws Exception {
        Long group = groupId("GRP-INTELLIGENCE");
        LocalDateTime submitTime = LocalDateTime.of(2026, 8, 1, 9, 0);
        buildProject(group, "预检对照项目", submitTime);
        long target = buildProject(group, "预检目标项目", submitTime);
        String admin = adminToken();
        saveConfig(admin, 70, 30);
        doReturn(LlmCallResult.success(new LlmResponse(88D, Arrays.asList("模型重叠点"),
                        Arrays.asList("大模型分析差异点"), "EXPLAIN", "模型复核结论"), 10L, 8, 4))
                .when(llmClient).call(any(), any(), any());

        MvcResult result = mockMvc.perform(post("/api/v1/analysis/precheck")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + target + "}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = readJson(result).path("data");
        assertThat(data.path("llmUsed").asBoolean()).isTrue();
        JsonNode top = data.path("topPeers").get(0);
        assertThat(top.path("llmReason").asText()).isEqualTo("模型复核结论");
        assertThat(top.path("totalScore").decimalValue())
                .isEqualByComparingTo(top.path("localScore").decimalValue());
        assertThat(top.path("llmOverlaps").get(0).asText()).isEqualTo("模型重叠点");
    }

    @Test
    @DisplayName("UT-L01/L02 连接自检成功、失败与权限")
    void ut22ConnectionTest() throws Exception {
        doReturn(LlmCallResult.success(new LlmResponse(50D, Arrays.asList(), Arrays.asList(),
                        "EXPLAIN", "连接测试"), 12L, 3, 2))
                .when(llmClient).call(any(), any(), any());
        JsonNode ok = readJson(mockMvc.perform(post("/api/v1/system/llm/test")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk()).andReturn()).path("data");
        assertThat(ok.path("success").asBoolean()).isTrue();
        assertThat(ok.path("model").asText()).isEqualTo("test-model");
        assertThat(ok.toString()).doesNotContain("apiKey");

        doReturn(LlmCallResult.failure(LlmStatus.TIMEOUT, 15L, "TIMEOUT"))
                .when(llmClient).call(any(), any(), any());
        JsonNode failed = readJson(mockMvc.perform(post("/api/v1/system/llm/test")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk()).andReturn()).path("data");
        assertThat(failed.path("success").asBoolean()).isFalse();
        assertThat(failed.path("status").asText()).isEqualTo("TIMEOUT");

        mockMvc.perform(post("/api/v1/system/llm/test")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(status().isForbidden());
    }

    private void saveConfig(String token, int localWeight, int llmWeight) throws Exception {
        String body = "{"
                + "\"fieldWeights\":{\"goal\":15,\"content\":35,\"tech\":35,\"milestone\":5,\"tag\":10},"
                + "\"thresholds\":{\"high\":80,\"medium\":60,\"low\":40},"
                + "\"topK\":5,\"simultaneousDays\":30,\"llmEnabled\":true,"
                + "\"localWeight\":" + localWeight + ",\"llmWeight\":" + llmWeight + ","
                + "\"llmTopN\":10,\"llmTimeoutMs\":8000}";
        MvcResult result = mockMvc.perform(put("/api/v1/analysis/config")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(result).path("data").path("version").asInt()).isGreaterThan(1);
    }
}

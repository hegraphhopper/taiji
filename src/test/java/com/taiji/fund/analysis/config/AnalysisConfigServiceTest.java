package com.taiji.fund.analysis.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.AbstractApiTest;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.system.audit.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnalysisConfigServiceTest extends AbstractApiTest {

    @Autowired
    private AnalysisConfigMapper configMapper;

    @Test
    @Order(1)
    @DisplayName("UT-01 默认配置随迁移初始化且登录用户可读")
    void ut01_defaultConfig() throws Exception {
        AnalysisConfig version1 = configMapper.selectOne(Wrappers.<AnalysisConfig>lambdaQuery()
                .eq(AnalysisConfig::getVersion, 1));
        assertThat(version1).isNotNull();
        assertThat(version1.getFieldWeights())
                .isEqualTo("{\"goal\":15,\"content\":35,\"tech\":35,\"milestone\":5,\"tag\":10}");
        assertThat(version1.getThresholds())
                .isEqualTo("{\"high\":80,\"medium\":60,\"low\":40}");
        assertThat(version1.getTopK()).isEqualTo(5);
        assertThat(version1.getSimultaneousDays()).isEqualTo(30);
        assertThat(version1.getLlmEnabled()).isZero();
        assertThat(version1.getLocalWeight()).isEqualTo(70);
        assertThat(version1.getLlmWeight()).isEqualTo(30);
        assertThat(version1.getActive()).isEqualTo(1);

        String token = loginToken("user01", "User@12345");
        MvcResult result = mockMvc.perform(get("/api/v1/analysis/config")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = readJson(result);
        assertThat(body.path("code").asInt()).isZero();
        assertThat(body.path("data").path("version").asInt()).isEqualTo(1);
        assertThat(body.path("data").path("fieldWeights").path("goal").asInt()).isEqualTo(15);
        assertThat(body.path("data").path("thresholds").path("high").asInt()).isEqualTo(80);
    }

    @Test
    @Order(2)
    @DisplayName("UT-02 保存生成新版本并失效旧版本，写审计")
    void ut02_saveCreatesNewVersion() throws Exception {
        int previousVersion = maxVersion();
        String token = loginToken("admin", "Admin@123");
        int auditBefore = auditCount();

        MvcResult result = mockMvc.perform(put("/api/v1/analysis/config")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(20, 30, 30, 10, 10, 50, 70, 90,
                                8, 45, 1, 50, 3, 6000)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = readJson(result).path("data");
        int newVersion = data.path("version").asInt();
        assertThat(newVersion).isEqualTo(previousVersion + 1);
        assertThat(data.path("active").asBoolean()).isTrue();
        assertThat(data.path("fieldWeights").path("content").asInt()).isEqualTo(30);
        assertThat(data.path("llmEnabled").asBoolean()).isTrue();

        List<AnalysisConfig> active = configMapper.selectList(Wrappers.<AnalysisConfig>lambdaQuery()
                .eq(AnalysisConfig::getActive, 1));
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getVersion()).isEqualTo(newVersion);
        AnalysisConfig previous = configMapper.selectOne(Wrappers.<AnalysisConfig>lambdaQuery()
                .eq(AnalysisConfig::getVersion, previousVersion));
        assertThat(previous.getActive()).isZero();

        MvcResult historyResult = mockMvc.perform(get("/api/v1/analysis/config/history")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode history = readJson(historyResult).path("data");
        assertThat(history.isArray()).isTrue();
        assertThat(history.get(0).path("version").asInt()).isEqualTo(newVersion);

        assertThat(auditCount()).isGreaterThan(auditBefore);
        AuditLog latest = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                        .eq(AuditLog::getAction, "ANALYSIS_CONFIG_UPDATE")
                        .orderByDesc(AuditLog::getId))
                .get(0);
        assertThat(latest.getResourceType()).isEqualTo("ANALYSIS_CONFIG");
        assertThat(latest.getResult()).isEqualTo("SUCCESS");
        assertThat(latest.getUsername()).isEqualTo("admin");
    }

    @Test
    @Order(3)
    @DisplayName("UT-03 非法权重、阈值与范围参数返回 400 且不产生新版本")
    void ut03_invalidConfigRejected() throws Exception {
        String token = loginToken("admin", "Admin@123");
        List<String> invalidBodies = java.util.Arrays.asList(
                validJson(0, 0, 0, 0, 0, 40, 60, 80, 5, 30, 70, 30, 5, 8000),
                validJson(15, 35, 35, 5, 10, 60, 40, 80, 5, 30, 70, 30, 5, 8000),
                validJson(15, 35, 35, 5, 10, 40, 60, 101, 5, 30, 70, 30, 5, 8000),
                validJson(15, 35, 35, 5, 10, 40, 60, 80, 0, 30, 70, 30, 5, 8000),
                validJson(15, 35, 35, 5, 10, 40, 60, 80, 5, -1, 70, 30, 5, 8000),
                validJson(15, 35, 35, 5, 10, 40, 60, 80, 5, 30, 0, 0, 5, 8000),
                validJson(15, 35, 35, 5, 10, 40, 60, 80, 5, 30, 70, 30, 0, 8000),
                validJson(15, 35, 35, 5, 10, 40, 60, 80, 5, 30, 70, 30, 5, 499));

        for (String invalidBody : invalidBodies) {
            int versionBefore = maxVersion();
            MvcResult result = mockMvc.perform(put("/api/v1/analysis/config")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andReturn();
            int code = readJson(result).path("code").asInt();
            assertThat(code).isIn(ErrorCode.ANALYSIS_CONFIG_INVALID.getCode(),
                    ErrorCode.ANALYSIS_WEIGHT_INVALID.getCode(),
                    ErrorCode.ANALYSIS_THRESHOLD_INVALID.getCode());
            assertThat(maxVersion()).isEqualTo(versionBefore);
        }
    }

    @Test
    @Order(4)
    @DisplayName("UT-04 非管理员不可修改配置或查看历史")
    void ut04_nonAdminForbidden() throws Exception {
        int versionBefore = maxVersion();
        String pmoToken = loginToken("pmo01", "Pmo@12345");
        String userToken = loginToken("user01", "User@12345");
        String body = validJson(15, 35, 35, 5, 10, 40, 60, 80, 5, 30, 70, 30, 5, 8000);

        mockMvc.perform(put("/api/v1/analysis/config")
                        .header("Authorization", bearer(pmoToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/analysis/config")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/analysis/config/history")
                        .header("Authorization", bearer(pmoToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/analysis/config/history")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        assertThat(maxVersion()).isEqualTo(versionBefore);
    }

    private int maxVersion() {
        List<AnalysisConfig> configs = configMapper.selectList(Wrappers.<AnalysisConfig>lambdaQuery()
                .orderByDesc(AnalysisConfig::getVersion));
        return configs.isEmpty() ? 0 : configs.get(0).getVersion();
    }

    private int auditCount() {
        return Math.toIntExact(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, "ANALYSIS_CONFIG_UPDATE")));
    }

    private String validJson(int goal, int content, int tech, int milestone, int tag,
                             int low, int medium, int high,
                             int topK, int simultaneousDays,
                             int localWeight, int llmWeight, int llmTopN, int llmTimeoutMs)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("goal", goal);
        weights.put("content", content);
        weights.put("tech", tech);
        weights.put("milestone", milestone);
        weights.put("tag", tag);
        Map<String, Integer> thresholds = new LinkedHashMap<>();
        thresholds.put("high", high);
        thresholds.put("medium", medium);
        thresholds.put("low", low);
        body.put("fieldWeights", weights);
        body.put("thresholds", thresholds);
        body.put("topK", topK);
        body.put("simultaneousDays", simultaneousDays);
        body.put("llmEnabled", true);
        body.put("localWeight", localWeight);
        body.put("llmWeight", llmWeight);
        body.put("llmTopN", llmTopN);
        body.put("llmTimeoutMs", llmTimeoutMs);
        return objectMapper.writeValueAsString(body);
    }
}

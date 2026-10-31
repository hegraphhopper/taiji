package com.taiji.fund.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.AbstractAnalysisTest;
import com.taiji.fund.decision.mapper.DecisionRecordMapper;
import com.taiji.fund.decision.mapper.DecisionRuleMapper;
import com.taiji.fund.project.mapper.ProjectReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B2 决策测试基类：预检、保存记录、下载报告与夹具构造。
 */
public abstract class AbstractDecisionTest extends AbstractAnalysisTest {

    @Autowired
    protected DecisionRuleMapper decisionRuleMapper;

    @Autowired
    protected DecisionRecordMapper decisionRecordMapper;

    @Autowired
    protected ProjectReviewMapper projectReviewMapper;

    @Autowired
    protected DecisionRuleService decisionRuleService;

    protected String uniqueText(String prefix) {
        return prefix + "-" + System.nanoTime() + "-量子加密网关协同推理";
    }

    protected long submittedProject(Long groupId, String name, String text) {
        return buildProject(groupId, name, text, text, text + " 边缘计算 知识图谱",
                "原型开发", LocalDateHolder.START, LocalDateHolder.END,
                new BigDecimal("1000000.00"), java.time.LocalDateTime.now().minusDays(10),
                com.taiji.fund.common.enums.ProjectStatus.SUBMITTED.getCode());
    }

    protected long draftProject(Long groupId, String name, String text) {
        return buildProject(groupId, name, text, text, text + " 边缘计算 知识图谱",
                "原型开发", LocalDateHolder.START, LocalDateHolder.END,
                new BigDecimal("1000000.00"), null,
                com.taiji.fund.common.enums.ProjectStatus.DRAFT.getCode());
    }

    protected JsonNode precheck(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decision/precheck")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected JsonNode saveDecision(String token, JsonNode precheck) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", precheck.path("projectId").isNull()
                ? null : precheck.path("projectId").asLong());
        body.put("runId", precheck.path("runId").asLong());
        body.put("riskScore", precheck.path("riskScore").decimalValue());
        body.put("level", precheck.path("level").asText());
        body.put("recommendation", precheck.path("recommendation").asText());
        body.put("reason", precheck.path("reason").asText());
        body.put("similarProjects", objectMapper.writeValueAsString(precheck.path("topPeers")));
        if (!precheck.path("savingsEstimate").isNull()
                && !precheck.path("savingsEstimate").isMissingNode()) {
            body.put("savingsEstimate", precheck.path("savingsEstimate").decimalValue());
        }
        if (!precheck.path("inputHash").isNull()
                && !precheck.path("inputHash").isMissingNode()) {
            body.put("inputHash", precheck.path("inputHash").asText());
        }
        MvcResult result = mockMvc.perform(post("/api/v1/decision/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected JsonNode records(String token, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/decision/records?" + query)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected JsonNode recordDetail(String token, long id) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/decision/records/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected void voidRecord(String token, long id, int expectedStatus) throws Exception {
        mockMvc.perform(delete("/api/v1/decision/records/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().is(expectedStatus));
    }

    protected MvcResult download(String token, long id, String type) throws Exception {
        return mockMvc.perform(get("/api/v1/decision/records/{id}/report." + type, id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * 固定日期，避免年份漂移影响分析语料。
     */
    protected static final class LocalDateHolder {
        protected static final java.time.LocalDate START = java.time.LocalDate.of(2026, 1, 1);
        protected static final java.time.LocalDate END = java.time.LocalDate.of(2026, 12, 31);

        private LocalDateHolder() {
        }
    }
}

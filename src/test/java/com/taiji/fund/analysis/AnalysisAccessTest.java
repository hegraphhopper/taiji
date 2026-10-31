package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS,
        methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class AnalysisAccessTest extends AbstractAnalysisTest {

    @Test
    @DisplayName("UT-27 项目报告与项目对明细按数据范围校验")
    void ut27DataScope() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        Long groupB = groupId("GRP-DATA");
        LocalDateTime submitTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        long projectA = buildProject(groupA, "本组已提交项目", submitTime);
        long projectB = buildProject(groupB, "他组项目", submitTime);
        long draftA = buildProject(groupA, "本组草稿项目", DEFAULT_GOAL, DEFAULT_CONTENT,
                DEFAULT_TECH, DEFAULT_MILESTONE, java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 12, 31), new java.math.BigDecimal("100000.00"),
                null, com.taiji.fund.common.enums.ProjectStatus.DRAFT.getCode());

        MvcResult runResult = triggerRun(adminToken(), "{\"triggerType\":\"FULL\"}");
        assertThat(runResult.getResponse().getStatus()).isEqualTo(200);
        long runId = readJson(runResult).path("data").path("id").asLong();

        User groupUser = createGroupUser("access-group", groupA);
        String groupToken = tokenOf(groupUser);
        JsonNode ownReport = getData(groupToken, "/api/v1/analysis/projects/" + projectA, 200);
        assertThat(ownReport.path("project").path("id").asLong()).isEqualTo(projectA);
        assertThat(ownReport.path("analyzed").asBoolean()).isTrue();
        assertThat(ownReport.path("snapshot").path("maxScore").asDouble()).isGreaterThan(0D);
        assertThat(ownReport.path("topPeers").size()).isGreaterThan(0);

        getData(groupToken, "/api/v1/analysis/projects/" + projectB, 403);
        getData(pmoToken(), "/api/v1/analysis/projects/" + projectB, 200);

        User expert = createUser("access-expert", PASSWORD, UserStatus.ENABLED, groupA,
                RoleCode.EXPERT, 0);
        String expertToken = tokenOf(expert);
        getData(expertToken, "/api/v1/analysis/projects/" + projectA, 200);
        getData(expertToken, "/api/v1/analysis/projects/" + draftA, 403);

        MvcResult anonymous = mockMvc.perform(get("/api/v1/analysis/projects/{id}", projectA))
                .andReturn();
        assertThat(anonymous.getResponse().getStatus()).isEqualTo(401);

        List<AnalysisPair> pairs = pairsOfRun(runId);
        assertThat(pairs).isNotEmpty();
        Long pairId = pairs.get(0).getId();
        JsonNode pairDetail = getData(groupToken, "/api/v1/analysis/pairs/" + pairId, 200);
        assertThat(pairDetail.path("pairId").asLong()).isEqualTo(pairId);
        assertThat(pairDetail.path("project").path("id").asLong()).isNotNull();
        assertThat(pairDetail.path("scores").path("goal").asDouble()).isGreaterThanOrEqualTo(0D);
    }

    private JsonNode getData(String token, String path, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", bearer(token))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return readJson(result).path("data");
    }
}

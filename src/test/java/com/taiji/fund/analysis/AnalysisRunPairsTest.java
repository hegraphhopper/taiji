package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS,
        methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class AnalysisRunPairsTest extends AbstractAnalysisTest {

    @Test
    @DisplayName("UT-08 运行报告返回已落库项目对与可解释片段")
    void ut08RunPairsReturnsPersistedPairs() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        Long groupB = groupId("GRP-DATA");
        LocalDateTime submitTime = LocalDateTime.of(2026, 8, 1, 9, 0);
        buildProject(groupA, "报告项目A", submitTime);
        buildProject(groupA, "报告项目B", submitTime);
        buildProject(groupB, "报告项目C", submitTime.plusDays(10));

        JsonNode run = triggerRunData(pmoToken(), "{\"triggerType\":\"FULL\"}").path("data");
        long runId = run.path("id").asLong();

        JsonNode pairs = getData(pmoToken(), "/api/v1/analysis/runs/" + runId + "/pairs", 200);
        assertThat(pairs.isArray()).isTrue();
        assertThat(pairs.size()).isEqualTo(3);
        JsonNode first = pairs.get(0);
        assertThat(first.path("pairId").asLong()).isPositive();
        assertThat(first.path("project").path("name").asText()).isNotBlank();
        assertThat(first.path("peer").path("name").asText()).isNotBlank();
        assertThat(first.path("totalScore").asDouble()).isEqualTo(100D);
        assertThat(first.path("level").asText()).isEqualTo("HIGH");
        assertThat(first.path("direction").asText())
                .isIn("SELF_GROUP", "SIMULTANEOUS", "A_EARLIER", "B_EARLIER");
        assertThat(first.path("scores").path("goal").asDouble()).isPositive();
        assertThat(first.path("matchedPoints").size()).isPositive();
        assertThat(first.path("diffPoints").isArray()).isTrue();
    }

    @Test
    @DisplayName("UT-09 运行报告仅 PMO/ADMIN 可访问")
    void ut09RunPairsPermission() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        buildProject(groupA, "权限项目A", LocalDateTime.of(2026, 9, 1, 9, 0));
        buildProject(groupA, "权限项目B", LocalDateTime.of(2026, 9, 1, 9, 0));
        long runId = triggerRunData(pmoToken(), "{\"triggerType\":\"FULL\"}")
                .path("data").path("id").asLong();

        User groupUser = createGroupUser("run-report-group", groupA);
        getData(tokenOf(groupUser), "/api/v1/analysis/runs/" + runId + "/pairs", 403);
        User expert = createUser("run-report-expert", PASSWORD, UserStatus.ENABLED, groupA,
                RoleCode.EXPERT, 0);
        getData(tokenOf(expert), "/api/v1/analysis/runs/" + runId + "/pairs", 403);
        getData(adminToken(), "/api/v1/analysis/runs/" + runId + "/pairs", 200);

        MvcResult anonymous = mockMvc.perform(get("/api/v1/analysis/runs/{id}/pairs", runId))
                .andReturn();
        assertThat(anonymous.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("UT-10 运行不存在返回 404")
    void ut10RunNotFound() throws Exception {
        getData(pmoToken(), "/api/v1/analysis/runs/99999999/pairs", 404);
    }

    @Test
    @DisplayName("UT-11 无项目对的运行返回空数组")
    void ut11EmptyRun() throws Exception {
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        JsonNode pairs = getData(pmoToken(),
                "/api/v1/analysis/runs/" + run.getId() + "/pairs", 200);
        assertThat(pairs.isArray()).isTrue();
        assertThat(pairs.size()).isZero();
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

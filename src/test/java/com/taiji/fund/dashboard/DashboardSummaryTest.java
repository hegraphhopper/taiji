package com.taiji.fund.dashboard;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.AbstractAnalysisTest;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS,
        methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class DashboardSummaryTest extends AbstractAnalysisTest {

    private static final LocalDateTime SUBMIT_TIME = LocalDateTime.of(2026, 7, 1, 9, 0);

    @BeforeEach
    void cleanAnalysisData() {
        snapshotMapper.delete(Wrappers.<DuplicateSnapshot>lambdaQuery());
        analysisPairMapper.delete(Wrappers.<AnalysisPair>lambdaQuery());
        analysisRunMapper.delete(Wrappers.<AnalysisRun>lambdaQuery());
        projectMapper.delete(Wrappers.<Project>lambdaQuery());
    }

    @Test
    @Order(1)
    @DisplayName("UT-05 空库汇总为 0 且平均分为空")
    void ut05EmptyDatabase() throws Exception {
        JsonNode data = getData(pmoToken());
        assertThat(data.path("projectCount").asInt()).isZero();
        assertThat(data.path("analyzedCount").asInt()).isZero();
        assertThat(data.path("avgMaxScore").isNull()).isTrue();
        assertThat(data.path("highRiskCount").asInt()).isZero();
        assertThat(data.path("thresholds").path("high").asInt()).isEqualTo(80);
    }

    @Test
    @Order(2)
    @DisplayName("UT-01 PMO 汇总全部可见项目")
    void ut01PmoSummary() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        buildProject(groupA, "汇总项目A", SUBMIT_TIME);
        buildProject(groupA, "汇总项目B", SUBMIT_TIME);
        buildProject(groupA, "汇总项目C", SUBMIT_TIME);
        triggerRunData(pmoToken(), "{\"triggerType\":\"FULL\"}");

        JsonNode data = getData(pmoToken());
        assertThat(data.path("projectCount").asInt()).isEqualTo(3);
        assertThat(data.path("analyzedCount").asInt()).isEqualTo(3);
        assertThat(data.path("avgMaxScore").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(data.path("highRiskCount").asInt()).isEqualTo(3);
    }

    @Test
    @Order(3)
    @DisplayName("UT-02 项目组用户只统计本组")
    void ut02GroupUserScope() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        Long groupB = groupId("GRP-DATA");
        buildProject(groupA, "本组项目A", SUBMIT_TIME);
        buildProject(groupA, "本组项目B", SUBMIT_TIME);
        buildProject(groupB, "他组项目C", SUBMIT_TIME);
        triggerRunData(adminToken(), "{\"triggerType\":\"FULL\"}");

        User groupUser = createGroupUser("dashboard-group", groupA);
        JsonNode data = getData(tokenOf(groupUser));
        assertThat(data.path("projectCount").asInt()).isEqualTo(2);
        assertThat(data.path("analyzedCount").asInt()).isEqualTo(2);
        assertThat(data.path("highRiskCount").asInt()).isEqualTo(2);
    }

    @Test
    @Order(4)
    @DisplayName("UT-03 专家按可查看规则统计")
    void ut03ExpertScope() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        Long groupB = groupId("GRP-DATA");
        buildProject(groupA, "专家组内已提交", DEFAULT_GOAL, DEFAULT_CONTENT, DEFAULT_TECH,
                DEFAULT_MILESTONE, new java.math.BigDecimal("1000000.00"), SUBMIT_TIME,
                ProjectStatus.SUBMITTED.getCode());
        buildProject(groupB, "专家组外评审中", DEFAULT_GOAL, DEFAULT_CONTENT, DEFAULT_TECH,
                DEFAULT_MILESTONE, new java.math.BigDecimal("1000000.00"), SUBMIT_TIME,
                ProjectStatus.REVIEWING.getCode());
        buildProject(groupA, "专家的组内草稿", DEFAULT_GOAL, DEFAULT_CONTENT, DEFAULT_TECH,
                DEFAULT_MILESTONE, new java.math.BigDecimal("1000000.00"), SUBMIT_TIME,
                ProjectStatus.DRAFT.getCode());
        triggerRunData(adminToken(), "{\"triggerType\":\"FULL\"}");

        User expert = createUser("dashboard-expert", PASSWORD, UserStatus.ENABLED, groupA,
                RoleCode.EXPERT, 0);
        JsonNode data = getData(tokenOf(expert));
        assertThat(data.path("projectCount").asInt()).isEqualTo(2);
        assertThat(data.path("analyzedCount").asInt()).isEqualTo(2);
    }

    @Test
    @Order(5)
    @DisplayName("UT-04 有项目但未分析时平均分为空")
    void ut04NoSnapshots() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        buildProject(groupA, "未分析项目", SUBMIT_TIME);

        JsonNode data = getData(pmoToken());
        assertThat(data.path("projectCount").asInt()).isEqualTo(1);
        assertThat(data.path("analyzedCount").asInt()).isZero();
        assertThat(data.path("avgMaxScore").isNull()).isTrue();
        assertThat(data.path("highRiskCount").asInt()).isZero();
    }

    private JsonNode getData(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", bearer(token))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }
}

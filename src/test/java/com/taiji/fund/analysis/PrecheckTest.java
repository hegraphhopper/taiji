package com.taiji.fund.analysis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.RunStatus;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.system.audit.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PrecheckTest extends AbstractAnalysisTest {

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("UT-25 草稿项目预检返回 Top-K 与建议动作，不回写项目分")
    void ut25DraftPrecheckDoesNotWriteScore() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        long submitted = buildProject(groupA, "已提交对照项目", LocalDateTime.of(2026, 5, 1, 9, 0));
        long draft = buildProject(groupA, "草稿预检项目", DEFAULT_GOAL, DEFAULT_CONTENT, DEFAULT_TECH,
                DEFAULT_MILESTONE, java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 12, 31),
                new java.math.BigDecimal("500000.00"), null, ProjectStatus.DRAFT.getCode());

        String token = tokenOf(createGroupUser("precheck-user", groupA));
        MvcResult result = mockMvc.perform(post("/api/v1/analysis/precheck")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + draft + "}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = readJson(result).path("data");
        JsonNode topPeers = data.path("topPeers");
        assertThat(topPeers.size()).isGreaterThan(0);
        JsonNode first = topPeers.get(0);
        assertThat(first.path("peer").path("id").asLong()).isEqualTo(submitted);
        assertThat(first.path("suggestion").asText()).isIn("MERGE", "EXPLAIN", "APPROVE");
        assertThat(first.path("scores").path("goal").asDouble()).isGreaterThan(0D);
        assertThat(first.path("matchedPoints").size()).isGreaterThan(0);

        Project draftAfter = projectMapper.selectById(draft);
        assertThat(draftAfter.getDuplicateScore()).isNull();
        assertThat(draftAfter.getRiskLevel()).isNull();

        AnalysisRun run = analysisRunMapper.selectById(data.path("runId").asLong());
        assertThat(run.getTriggerType()).isEqualTo("PRECHECK");
        assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCESS.name());
        assertThat(run.getPairCount()).isEqualTo(topPeers.size());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("UT-25 临时文本预检可用且审计不记录正文")
    void ut25TextPrecheckAndAuditMasking() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        buildProject(groupA, "文本对照项目", LocalDateTime.of(2026, 5, 2, 9, 0));

        String secretText = "临时预检专用目标文本XYZ";
        String body = "{\"goalText\":\"" + secretText + "\","
                + "\"contentText\":\"" + DEFAULT_CONTENT + "\","
                + "\"techText\":\"" + DEFAULT_TECH + "\"}";
        String token = tokenOf(createGroupUser("precheck-text", groupA));
        MvcResult result = mockMvc.perform(post("/api/v1/analysis/precheck")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = readJson(result).path("data");
        assertThat(data.path("projectId").isNull()).isTrue();
        assertThat(data.path("topPeers").size()).isGreaterThan(0);

        List<AuditLog> logs = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, "ANALYSIS_PRECHECK")
                .orderByDesc(AuditLog::getId));
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getDetail()).doesNotContain(secretText).doesNotContain(DEFAULT_CONTENT);
    }
}

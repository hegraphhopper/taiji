package com.taiji.fund.analysis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.RunStatus;
import com.taiji.fund.system.audit.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class RunDeleteTest extends AbstractAnalysisTest {

    @Test
    @DisplayName("UT-28 仅 ADMIN 可删除失败运行且级联清理明细")
    void ut28DeleteFailedRunCascade() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        long projectA = buildProject(groupA, "删除测试项目A", LocalDateTime.of(2026, 7, 1, 9, 0));
        long projectB = buildProject(groupA, "删除测试项目B", LocalDateTime.of(2026, 7, 1, 9, 0));

        AnalysisRun failed = insertRun("FULL", RunStatus.FAILED.name(), null);
        failed.setMessage("模拟失败");
        analysisRunMapper.updateById(failed);
        insertPair(failed.getId(), projectA, projectB);
        insertSnapshot(failed.getId(), projectA, groupA);

        AnalysisRun success = insertRun("FULL", RunStatus.SUCCESS.name(), null);

        MvcResult pmoDenied = mockMvc.perform(delete("/api/v1/analysis/runs/{id}", failed.getId())
                        .header("Authorization", bearer(pmoToken())))
                .andReturn();
        assertThat(pmoDenied.getResponse().getStatus()).isEqualTo(403);
        assertThat(analysisRunMapper.selectById(failed.getId())).isNotNull();

        MvcResult successDenied = mockMvc.perform(delete("/api/v1/analysis/runs/{id}", success.getId())
                        .header("Authorization", bearer(adminToken())))
                .andReturn();
        assertThat(successDenied.getResponse().getStatus()).isEqualTo(400);

        MvcResult deleted = mockMvc.perform(delete("/api/v1/analysis/runs/{id}", failed.getId())
                        .header("Authorization", bearer(adminToken())))
                .andReturn();
        assertThat(deleted.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = readJson(deleted);
        assertThat(body.path("code").asInt()).isZero();

        assertThat(analysisRunMapper.selectById(failed.getId())).isNull();
        assertThat(analysisPairMapper.selectCount(Wrappers.<AnalysisPair>lambdaQuery()
                .eq(AnalysisPair::getRunId, failed.getId()))).isZero();
        assertThat(snapshotMapper.selectCount(Wrappers.<DuplicateSnapshot>lambdaQuery()
                .eq(DuplicateSnapshot::getRunId, failed.getId()))).isZero();
        assertThat(analysisRunMapper.selectById(success.getId())).isNotNull();

        List<AuditLog> logs = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, "ANALYSIS_RUN_DELETE")
                .orderByDesc(AuditLog::getId));
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getResult()).isEqualTo("SUCCESS");
    }

    private void insertPair(Long runId, Long projectId, Long peerId) {
        AnalysisPair pair = new AnalysisPair();
        pair.setRunId(runId);
        pair.setProjectId(projectId);
        pair.setPeerProjectId(peerId);
        pair.setGoalScore(new BigDecimal("50.00"));
        pair.setContentScore(new BigDecimal("50.00"));
        pair.setTechScore(new BigDecimal("50.00"));
        pair.setMilestoneScore(new BigDecimal("50.00"));
        pair.setTagScore(new BigDecimal("50.00"));
        pair.setLocalScore(new BigDecimal("50.00"));
        pair.setTotalScore(new BigDecimal("50.00"));
        pair.setLevel("LOW");
        pair.setRelation("SELF_GROUP");
        pair.setDirection("SELF_GROUP");
        pair.setMatchedPoints("[]");
        pair.setDiffPoints("[]");
        analysisPairMapper.insert(pair);
    }

    private void insertSnapshot(Long runId, Long projectId, Long groupId) {
        DuplicateSnapshot snapshot = new DuplicateSnapshot();
        snapshot.setRunId(runId);
        snapshot.setProjectId(projectId);
        snapshot.setGroupId(groupId);
        snapshot.setAnalysisDate(LocalDate.now());
        snapshot.setMaxScore(new BigDecimal("50.00"));
        snapshot.setAvgTopScore(new BigDecimal("50.00"));
        snapshot.setHighPairCount(0);
        snapshot.setCrossGroupCount(0);
        snapshot.setSelfGroupCount(1);
        snapshot.setOurEarlierCount(0);
        snapshot.setPeerEarlierCount(0);
        snapshot.setSimultaneousCount(0);
        snapshotMapper.insert(snapshot);
    }
}

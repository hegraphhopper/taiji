package com.taiji.fund.insight.trend;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.AbstractAnalysisTest;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A4 趋势测试基类：通过 Mapper 直接造快照/项目对，按月份隔离互不干扰。
 */
public abstract class AbstractTrendTest extends AbstractAnalysisTest {

    protected Long intelGroupId() {
        return groupId("GRP-INTELLIGENCE");
    }

    protected Long dataGroupId() {
        return groupId("GRP-DATA");
    }

    protected Long newProject(Long groupId, String name) {
        return buildProject(groupId, name, java.time.LocalDateTime.now());
    }

    protected AnalysisRun newRun() {
        return insertRun("FULL", "SUCCESS", null);
    }

    protected DuplicateSnapshot snapshot(Long runId, Long projectId, Long groupId, LocalDate date,
                                         String max, String avgTop, int highPair,
                                         int self, int ourEarlier, int peerEarlier, int simultaneous) {
        DuplicateSnapshot snapshot = new DuplicateSnapshot();
        snapshot.setRunId(runId);
        snapshot.setProjectId(projectId);
        snapshot.setGroupId(groupId);
        snapshot.setAnalysisDate(date);
        snapshot.setMaxScore(new BigDecimal(max));
        snapshot.setAvgTopScore(new BigDecimal(avgTop));
        snapshot.setHighPairCount(highPair);
        snapshot.setSelfGroupCount(self);
        snapshot.setCrossGroupCount(ourEarlier + peerEarlier + simultaneous);
        snapshot.setOurEarlierCount(ourEarlier);
        snapshot.setPeerEarlierCount(peerEarlier);
        snapshot.setSimultaneousCount(simultaneous);
        snapshot.setRiskExposure(new BigDecimal("1000000.00"));
        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    protected AnalysisPair pair(Long runId, Long projectId, Long peerId, String level, String total) {
        long left = Math.min(projectId, peerId);
        long right = Math.max(projectId, peerId);
        AnalysisPair pair = new AnalysisPair();
        pair.setRunId(runId);
        pair.setProjectId(left);
        pair.setPeerProjectId(right);
        pair.setGoalScore(new BigDecimal("60.00"));
        pair.setContentScore(new BigDecimal("60.00"));
        pair.setTechScore(new BigDecimal("60.00"));
        pair.setMilestoneScore(new BigDecimal("60.00"));
        pair.setTagScore(new BigDecimal("60.00"));
        pair.setLocalScore(new BigDecimal(total));
        pair.setTotalScore(new BigDecimal(total));
        pair.setLevel(level);
        pair.setRelation("CROSS_GROUP");
        pair.setDirection("SIMULTANEOUS");
        pair.setEarlierProjectId(left);
        pair.setMatchedPoints("[]");
        pair.setDiffPoints("[]");
        analysisPairMapper.insert(pair);
        return pair;
    }

    protected JsonNode trend(String token, String query) throws Exception {
        return okData("/api/v1/insight/trend?" + query, token);
    }

    protected JsonNode attribution(String token, String query) throws Exception {
        return okData("/api/v1/insight/attribution?" + query, token);
    }

    protected JsonNode heatmap(String token, String query) throws Exception {
        return okData("/api/v1/insight/heatmap?" + query, token);
    }

    protected JsonNode drill(String token, String query) throws Exception {
        return okData("/api/v1/insight/trend/projects?" + query, token);
    }

    protected JsonNode okData(String url, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected JsonNode lastPoint(JsonNode data) {
        JsonNode points = data.path("points");
        return points.get(points.size() - 1);
    }
}

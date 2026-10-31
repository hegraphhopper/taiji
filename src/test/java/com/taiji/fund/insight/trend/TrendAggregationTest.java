package com.taiji.fund.insight.trend;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT-01/02/03/04/07/08：月份去重、跨月保留、指标聚合、项目视角对数与唯一对数、高风险口径、环比。
 */
class TrendAggregationTest extends AbstractTrendTest {

    @Test
    void ut01SameProjectSameMonthKeepsLatestRun() throws Exception {
        Long group = intelGroupId();
        Long projectId = newProject(group, "趋势去重项目");
        AnalysisRun first = newRun();
        AnalysisRun second = newRun();
        snapshot(first.getId(), projectId, group, LocalDate.of(2021, 5, 5), "50.00", "40.00", 0, 0, 0, 0, 0);
        snapshot(second.getId(), projectId, group, LocalDate.of(2021, 5, 25), "72.00", "60.00", 1, 0, 0, 0, 0);

        JsonNode data = trend(pmoToken(), "startMonth=2021-05&endMonth=2021-05");
        JsonNode point = lastPoint(data);
        assertEquals(1, point.path("analyzedProjectCount").asInt());
        assertEquals(72.0, point.path("avgMaxScore").asDouble(), 0.001);
        assertEquals(1, point.path("highRiskCount").asInt());
        assertEquals("2021-05", point.path("period").asText());
    }

    @Test
    void ut02KeepsHistoryAcrossMonths() throws Exception {
        Long group = intelGroupId();
        Long projectId = newProject(group, "跨月趋势项目");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectId, group, LocalDate.of(2021, 6, 10), "40.00", "30.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), projectId, group, LocalDate.of(2021, 7, 10), "45.00", "35.00", 0, 0, 0, 0, 0);

        JsonNode data = trend(pmoToken(), "startMonth=2021-06&endMonth=2021-07");
        assertEquals(2, data.path("points").size());
        assertEquals("2021-06", data.path("points").get(0).path("period").asText());
        assertEquals("2021-07", data.path("points").get(1).path("period").asText());
    }

    @Test
    void ut03AggregatesTrendMetrics() throws Exception {
        Long group = intelGroupId();
        AnalysisRun run = newRun();
        Long p1 = newProject(group, "指标项目一");
        Long p2 = newProject(group, "指标项目二");
        Long p3 = newProject(group, "指标项目三");
        snapshot(run.getId(), p1, group, LocalDate.of(2021, 8, 5), "80.00", "70.00", 1, 0, 0, 0, 0);
        snapshot(run.getId(), p2, group, LocalDate.of(2021, 8, 6), "60.00", "50.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), p3, group, LocalDate.of(2021, 8, 7), "40.00", "30.00", 0, 0, 0, 0, 0);

        JsonNode point = lastPoint(trend(pmoToken(), "startMonth=2021-08&endMonth=2021-08"));
        assertEquals(3, point.path("analyzedProjectCount").asInt());
        assertEquals(60.0, point.path("avgMaxScore").asDouble(), 0.001);
        assertEquals(50.0, point.path("avgTopScore").asDouble(), 0.001);
        assertEquals(1, point.path("highRiskCount").asInt());
        assertEquals(1, point.path("highPairCount").asInt());
    }

    @Test
    void ut04ProjectViewPairsAndUniquePairs() throws Exception {
        Long intel = intelGroupId();
        Long data = dataGroupId();
        Long projectA = newProject(intel, "我方在先项目");
        Long projectC = newProject(data, "他组项目");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectA, intel, LocalDate.of(2021, 9, 10), "88.00", "80.00", 1, 0, 1, 0, 0);
        snapshot(run.getId(), projectC, data, LocalDate.of(2021, 9, 10), "88.00", "80.00", 1, 0, 0, 1, 0);
        pair(run.getId(), projectA, projectC, "HIGH", "88.00");

        JsonNode dataNode = trend(pmoToken(), "startMonth=2021-09&endMonth=2021-09&groupId=" + intel);
        JsonNode point = lastPoint(dataNode);
        assertEquals(1, point.path("analyzedProjectCount").asInt());
        assertEquals(1, point.path("pairCount").asInt());
        assertEquals(1, point.path("uniquePairCount").asInt());
        assertEquals(1, point.path("attribution").path("ourEarlier").asInt());

        JsonNode bothGroups = trend(pmoToken(), "startMonth=2021-09&endMonth=2021-09");
        JsonNode combined = lastPoint(bothGroups);
        assertEquals(2, combined.path("analyzedProjectCount").asInt());
        assertEquals(2, combined.path("pairCount").asInt());
        assertEquals(1, combined.path("uniquePairCount").asInt());
        assertEquals(2, combined.path("attribution").path("ourEarlier").asInt()
                + combined.path("attribution").path("peerEarlier").asInt());
    }

    @Test
    void ut07HighRiskUsesStoredJudgement() throws Exception {
        Long group = intelGroupId();
        Long projectId = newProject(group, "高阈值项目");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectId, group, LocalDate.of(2021, 10, 8), "95.00", "90.00", 0, 0, 0, 0, 0);

        JsonNode point = lastPoint(trend(pmoToken(), "startMonth=2021-10&endMonth=2021-10&groupId=" + group));
        assertEquals(0, point.path("highRiskCount").asInt(),
                "高风险数必须按落库 high_pair_count 判定，不按当前阈值回算");
    }

    @Test
    void ut08MomComparesWithPreviousPoint() throws Exception {
        Long group = intelGroupId();
        Long projectId = newProject(group, "环比项目");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectId, group, LocalDate.of(2021, 11, 8), "50.00", "40.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), projectId, group, LocalDate.of(2021, 12, 8), "55.00", "45.00", 0, 0, 0, 0, 0);

        JsonNode data = trend(pmoToken(), "startMonth=2021-11&endMonth=2021-12");
        JsonNode first = data.path("points").get(0);
        JsonNode second = data.path("points").get(1);
        assertTrue(first.path("mom").isNull(), "首点环比应为 null");
        assertEquals(5.0, second.path("mom").path("avgMaxScoreDelta").asDouble(), 0.001);
        assertEquals(0.1, second.path("mom").path("avgMaxScoreRatio").asDouble(), 0.0001);
    }

    @Test
    void ut09EmptyRangeReturnsEmptyState() throws Exception {
        JsonNode data = trend(pmoToken(), "startMonth=2019-01&endMonth=2019-02");
        assertFalse(data.path("hasData").asBoolean());
        assertEquals(0, data.path("points").size());
        assertTrue(data.path("summary").size() > 0);
    }
}

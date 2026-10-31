package com.taiji.fund.insight.investment;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT-I01/I02/I03：KPI、敞口、未分析分母口径。
 */
class InvestmentOverviewTest extends AbstractInvestmentTest {

    @Test
    void ut01OverviewKpi() throws Exception {
        Long group = createGroup("投入概览组").getId();
        Long p1 = insertProject(group, "在研项目", "IN_PROGRESS", "PLATFORM",
                LocalDate.of(2031, 1, 1), "1000000");
        Long p2 = insertProject(group, "结项项目", "COMPLETED", "INNOVATION",
                LocalDate.of(2031, 2, 1), "2000000");
        Long p3 = insertProject(group, "已立项未分析", "APPROVED", "PLATFORM",
                LocalDate.of(2031, 3, 1), "500000");
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        snapshot(run.getId(), p1, group, LocalDate.of(2031, 6, 30), "80.00", "70.00", 1, 0, 0, 0, 0);
        snapshot(run.getId(), p2, group, LocalDate.of(2031, 6, 30), "60.00", "50.00", 0, 0, 0, 0, 0);

        JsonNode data = overview(adminToken(), "startYear=2031&endYear=2031&groupId=" + group);

        assertTrue(data.path("hasData").asBoolean());
        assertEquals(3, data.path("projectCount").asInt());
        assertEquals(1, data.path("inProgressCount").asInt());
        assertEquals(1, data.path("completedCount").asInt());
        assertEquals(0, data.path("totalBudget").decimalValue().compareTo(new java.math.BigDecimal("3500000.00")));
        assertEquals(0, data.path("avgBudget").decimalValue().compareTo(new java.math.BigDecimal("1166666.67")));
        assertEquals(2, data.path("analyzedCount").asInt());
        assertEquals(1, data.path("unanalyzedCount").asInt());
        assertEquals(70.0, data.path("avgMaxScore").asDouble(), 0.001);
        assertEquals(1, data.path("highRiskCount").asInt());
        assertFalse(p3 == null);
    }

    @Test
    void ut02ExposureAndHighExposure() throws Exception {
        Long group = createGroup("投入敞口组").getId();
        Long p1 = insertProject(group, "高风险项目", "IN_PROGRESS", "PLATFORM",
                LocalDate.of(2032, 1, 1), "1000000");
        Long p2 = insertProject(group, "中风险项目", "APPROVED", "PLATFORM",
                LocalDate.of(2032, 2, 1), "2000000");
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        snapshot(run.getId(), p1, group, LocalDate.of(2032, 6, 30), "85.00", "80.00", 1, 0, 0, 0, 0);
        snapshot(run.getId(), p2, group, LocalDate.of(2032, 6, 30), "60.00", "55.00", 0, 0, 0, 0, 0);

        JsonNode data = overview(adminToken(), "startYear=2032&endYear=2032&groupId=" + group);

        assertEquals(0, data.path("exposure").decimalValue()
                .compareTo(new java.math.BigDecimal("2050000.00")));
        assertEquals(0, data.path("highExposure").decimalValue()
                .compareTo(new java.math.BigDecimal("850000.00")));
        assertEquals(1, data.path("highRiskCount").asInt());
    }

    @Test
    void ut03UnanalyzedExcludedFromAverage() throws Exception {
        Long group = createGroup("未分析口径组").getId();
        Long analyzed = insertProject(group, "已分析项目", "APPROVED", "PLATFORM",
                LocalDate.of(2033, 1, 1), "800000");
        insertProject(group, "未分析一", "SUBMITTED", "PLATFORM",
                LocalDate.of(2033, 2, 1), "300000");
        insertProject(group, "未分析二", "REVIEWING", "PLATFORM",
                LocalDate.of(2033, 3, 1), "200000");
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        snapshot(run.getId(), analyzed, group, LocalDate.of(2033, 6, 30), "80.00", "78.00", 1, 0, 0, 0, 0);

        // SUBMITTED/REVIEWING 不属于投入统计范围，这里验证的是“纳入但无快照”的项目
        Long noSnapshot = insertProject(group, "已立项无快照", "APPROVED", "PLATFORM",
                LocalDate.of(2033, 4, 1), "400000");
        insertProject(group, "已立项无快照二", "IN_PROGRESS", "PLATFORM",
                LocalDate.of(2033, 5, 1), "600000");

        JsonNode data = overview(adminToken(), "startYear=2033&endYear=2033&groupId=" + group);

        assertEquals(3, data.path("projectCount").asInt(), "仅 APPROVED/IN_PROGRESS 纳入");
        assertEquals(1, data.path("analyzedCount").asInt());
        assertEquals(2, data.path("unanalyzedCount").asInt());
        assertEquals(80.0, data.path("avgMaxScore").asDouble(), 0.001);
        assertFalse(noSnapshot == null);
    }

    @Test
    void ut05StatusScopeAndIncludeCompleted() throws Exception {
        Long group = createGroup("状态范围组").getId();
        insertProject(group, "草稿", "DRAFT", "PLATFORM", LocalDate.of(2034, 1, 1), "100000");
        insertProject(group, "已提交", "SUBMITTED", "PLATFORM", LocalDate.of(2034, 1, 2), "100000");
        insertProject(group, "评审中", "REVIEWING", "PLATFORM", LocalDate.of(2034, 1, 3), "100000");
        insertProject(group, "已驳回", "REJECTED", "PLATFORM", LocalDate.of(2034, 1, 4), "100000");
        insertProject(group, "已立项", "APPROVED", "PLATFORM", LocalDate.of(2034, 2, 1), "100000");
        insertProject(group, "进行中", "IN_PROGRESS", "PLATFORM", LocalDate.of(2034, 2, 2), "100000");
        insertProject(group, "已结项", "COMPLETED", "PLATFORM", LocalDate.of(2034, 2, 3), "100000");

        JsonNode withCompleted = overview(adminToken(), "startYear=2034&endYear=2034&groupId=" + group);
        assertEquals(3, withCompleted.path("projectCount").asInt(), "默认纳入 APPROVED/IN_PROGRESS/COMPLETED");
        assertEquals(1, withCompleted.path("completedCount").asInt());

        JsonNode withoutCompleted = overview(adminToken(),
                "startYear=2034&endYear=2034&groupId=" + group + "&includeCompleted=false");
        assertEquals(2, withoutCompleted.path("projectCount").asInt(), "includeCompleted=false 排除结项");
        assertEquals(0, withoutCompleted.path("completedCount").asInt());
    }

    @Test
    void ut12EmptyRange() throws Exception {
        JsonNode data = overview(adminToken(), "startYear=2099&endYear=2099");
        assertFalse(data.path("hasData").asBoolean());
        assertEquals(0, data.path("projectCount").asInt());
        assertEquals(0, data.path("totalBudget").decimalValue()
                .compareTo(java.math.BigDecimal.ZERO));
    }
}

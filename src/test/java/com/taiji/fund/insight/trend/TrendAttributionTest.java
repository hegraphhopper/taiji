package com.taiji.fund.insight.trend;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT-05/06：四类归因计数、占比与按项目组矩阵。
 */
class TrendAttributionTest extends AbstractTrendTest {

    @Test
    void ut05CountsRatiosAndGroupMatrix() throws Exception {
        Long intel = intelGroupId();
        Long data = dataGroupId();
        Long projectA = newProject(intel, "归因项目A");
        Long projectB = newProject(data, "归因项目B");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectA, intel, LocalDate.of(2022, 1, 10), "70.00", "60.00",
                1, 3, 2, 0, 0);
        snapshot(run.getId(), projectB, data, LocalDate.of(2022, 1, 11), "70.00", "60.00",
                0, 0, 0, 1, 4);

        JsonNode dataNode = attribution(pmoToken(), "startMonth=2022-01&endMonth=2022-01");
        assertTrue(dataNode.path("hasData").asBoolean());
        assertEquals(10, dataNode.path("total").asInt());
        assertEquals(3, dataNode.path("counts").path("selfGroup").asInt());
        assertEquals(2, dataNode.path("counts").path("ourEarlier").asInt());
        assertEquals(1, dataNode.path("counts").path("peerEarlier").asInt());
        assertEquals(4, dataNode.path("counts").path("simultaneous").asInt());
        assertEquals(0.3, dataNode.path("ratios").path("selfGroup").asDouble(), 0.0001);
        assertEquals(0.2, dataNode.path("ratios").path("ourEarlier").asDouble(), 0.0001);
        assertEquals(0.1, dataNode.path("ratios").path("peerEarlier").asDouble(), 0.0001);
        assertEquals(0.4, dataNode.path("ratios").path("simultaneous").asDouble(), 0.0001);

        JsonNode groups = dataNode.path("groups");
        assertEquals(2, groups.size());
        boolean hasIntel = false;
        boolean hasData = false;
        for (JsonNode group : groups) {
            if (group.path("groupId").asLong() == intel) {
                hasIntel = true;
                assertEquals(5, group.path("total").asInt());
                assertEquals(3, group.path("counts").path("selfGroup").asInt());
                assertEquals(2, group.path("counts").path("ourEarlier").asInt());
            }
            if (group.path("groupId").asLong() == data) {
                hasData = true;
                assertEquals(5, group.path("total").asInt());
                assertEquals(1, group.path("counts").path("peerEarlier").asInt());
                assertEquals(4, group.path("counts").path("simultaneous").asInt());
            }
        }
        assertTrue(hasIntel && hasData);
    }

    @Test
    void ut05bGroupFilterApplies() throws Exception {
        Long intel = intelGroupId();
        Long data = dataGroupId();
        Long projectA = newProject(intel, "组过滤项目A");
        Long projectB = newProject(data, "组过滤项目B");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectA, intel, LocalDate.of(2021, 3, 10), "60.00", "50.00", 0, 2, 0, 0, 0);
        snapshot(run.getId(), projectB, data, LocalDate.of(2021, 3, 11), "60.00", "50.00", 0, 0, 3, 0, 0);

        JsonNode dataNode = attribution(pmoToken(),
                "startMonth=2021-03&endMonth=2021-03&groupId=" + intel);
        assertEquals(2, dataNode.path("total").asInt());
        assertEquals(1, dataNode.path("groups").size());
        assertEquals(intel.longValue(), dataNode.path("groups").get(0).path("groupId").asLong());
    }
}

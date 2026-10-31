package com.taiji.fund.insight.trend;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT-11/12：两种热力图维度与无数据单元格。
 */
class TrendHeatmapTest extends AbstractTrendTest {

    @Test
    void ut11GroupMonthHeatmap() throws Exception {
        Long intel = intelGroupId();
        Long data = dataGroupId();
        Long projectA = newProject(intel, "热力项目A");
        Long projectB = newProject(data, "热力项目B");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectA, intel, LocalDate.of(2022, 2, 10), "60.00", "50.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), projectB, data, LocalDate.of(2022, 2, 11), "40.00", "30.00", 0, 0, 0, 0, 0);

        JsonNode dataNode = heatmap(pmoToken(),
                "startMonth=2022-02&endMonth=2022-02&dimension=GROUP_MONTH");
        assertTrue(dataNode.path("hasData").asBoolean());
        assertEquals("GROUP_MONTH", dataNode.path("dimension").asText());
        assertEquals(1, dataNode.path("columns").size());
        assertEquals("2022-02", dataNode.path("columns").get(0).asText());
        assertEquals(2, dataNode.path("rows").size());
        JsonNode cells = dataNode.path("cells");
        assertEquals(2, cells.size());
        for (JsonNode cell : cells) {
            assertEquals(1, cell.path("sampleCount").asInt());
            if (cell.path("rowKey").asText().equals(String.valueOf(intel))) {
                assertEquals(60.0, cell.path("value").asDouble(), 0.001);
            } else {
                assertEquals(40.0, cell.path("value").asDouble(), 0.001);
            }
        }
    }

    @Test
    void ut12DirectionGroupHeatmap() throws Exception {
        Long intel = intelGroupId();
        Long data = dataGroupId();
        Long projectA = newProject(intel, "方向热力A");
        Long projectB = newProject(data, "方向热力B");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectA, intel, LocalDate.of(2021, 2, 10), "60.00", "50.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), projectB, data, LocalDate.of(2021, 2, 11), "40.00", "30.00", 0, 0, 0, 0, 0);

        JsonNode dataNode = heatmap(pmoToken(),
                "startMonth=2021-02&endMonth=2021-02&dimension=DIRECTION_GROUP");
        assertEquals("DIRECTION_GROUP", dataNode.path("dimension").asText());
        assertEquals(1, dataNode.path("rows").size(), "两个项目共用同一技术方向，应只有一行");
        assertEquals(2, dataNode.path("columns").size());
        assertEquals(2, dataNode.path("cells").size());
    }

    @Test
    void ut12bUnknownDimensionRejected() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/insight/heatmap?dimension=UNKNOWN")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }
}

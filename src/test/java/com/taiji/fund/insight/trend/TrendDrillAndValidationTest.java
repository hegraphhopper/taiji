package com.taiji.fund.insight.trend;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-10/14：下钻项目列表、等级过滤与查询参数校验。
 */
class TrendDrillAndValidationTest extends AbstractTrendTest {

    @Test
    void ut14DrillFiltersByLevel() throws Exception {
        Long group = intelGroupId();
        Long high = newProject(group, "下钻高风险项目");
        Long normal = newProject(group, "下钻普通项目");
        AnalysisRun run = newRun();
        snapshot(run.getId(), high, group, LocalDate.of(2022, 3, 10), "85.00", "80.00", 1, 0, 1, 0, 0);
        snapshot(run.getId(), normal, group, LocalDate.of(2022, 3, 11), "50.00", "40.00", 0, 0, 0, 0, 0);

        JsonNode data = drill(pmoToken(), "period=2022-03&startMonth=2022-03&endMonth=2022-03&level=HIGH");
        assertEquals(1, data.size());
        assertEquals(high.longValue(), data.get(0).path("projectId").asLong());
        assertEquals("HIGH", data.get(0).path("riskLevel").asText());
        assertEquals(85.0, data.get(0).path("maxScore").asDouble(), 0.001);

        JsonNode all = drill(pmoToken(), "period=2022-03&startMonth=2022-03&endMonth=2022-03");
        assertEquals(2, all.size());
        assertEquals(85.0, all.get(0).path("maxScore").asDouble(), 0.001, "按最高分倒序");
    }

    @Test
    void ut10RejectsSpanOver36Months() throws Exception {
        mockMvc.perform(get("/api/v1/insight/trend?startMonth=2020-01&endMonth=2023-02")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ut10bRejectsReversedRange() throws Exception {
        mockMvc.perform(get("/api/v1/insight/trend?startMonth=2022-05&endMonth=2022-01")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ut10cRejectsUnknownGranularity() throws Exception {
        mockMvc.perform(get("/api/v1/insight/trend?granularity=WEEK")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ut10dDrillRequiresPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/insight/trend/projects?startMonth=2022-03&endMonth=2022-03")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(status().isBadRequest());
    }
}

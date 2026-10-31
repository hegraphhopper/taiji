package com.taiji.fund.insight.investment;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-I08/I09/I11：Top 风险排序、下钻列表、Excel/CSV 导出。
 */
class InvestmentDrillAndExportTest extends AbstractInvestmentTest {

    @Test
    void ut08TopRiskOrderedByExposure() throws Exception {
        Long group = createGroup("Top敞口组").getId();
        Long high = insertProject(group, "高敞口", "APPROVED", "PLATFORM",
                LocalDate.of(2038, 1, 1), "2000000");
        Long mid = insertProject(group, "中敞口", "APPROVED", "PLATFORM",
                LocalDate.of(2038, 2, 1), "1000000");
        Long low = insertProject(group, "低敞口", "IN_PROGRESS", "PLATFORM",
                LocalDate.of(2038, 3, 1), "500000");
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        snapshot(run.getId(), high, group, LocalDate.of(2038, 6, 30), "90.00", "88.00", 1, 0, 0, 0, 0);
        snapshot(run.getId(), mid, group, LocalDate.of(2038, 6, 30), "50.00", "40.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), low, group, LocalDate.of(2038, 6, 30), "30.00", "20.00", 0, 0, 0, 0, 0);

        JsonNode items = investmentList(adminToken(), "top-risk", "groupId=" + group);
        assertEquals(3, items.size());
        assertEquals("高敞口", items.get(0).path("name").asText());
        assertEquals(0, items.get(0).path("exposure").decimalValue()
                .compareTo(new java.math.BigDecimal("1800000.00")));
        assertEquals("中敞口", items.get(1).path("name").asText());
        assertEquals("低敞口", items.get(2).path("name").asText());
    }

    @Test
    void ut09DrillProjectsAndUnanalyzedFlag() throws Exception {
        Long group = createGroup("下钻口径组").getId();
        Long analyzed = insertProject(group, "已分析", "APPROVED", "PLATFORM",
                LocalDate.of(2039, 1, 1), "1000000");
        Long unanalyzed = insertProject(group, "未分析", "IN_PROGRESS", "PLATFORM",
                LocalDate.of(2039, 2, 1), "800000");
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        snapshot(run.getId(), analyzed, group, LocalDate.of(2039, 6, 30), "70.00", "60.00", 0, 0, 0, 0, 0);

        JsonNode items = investmentList(adminToken(), "projects", "groupId=" + group);
        assertEquals(2, items.size());
        boolean hasAnalyzed = false;
        boolean hasUnanalyzed = false;
        for (JsonNode item : items) {
            if (item.path("projectId").asLong() == analyzed) {
                hasAnalyzed = true;
                assertEquals("MEDIUM", item.path("riskLevel").asText());
            }
            if (item.path("projectId").asLong() == unanalyzed) {
                hasUnanalyzed = true;
                assertEquals("UNANALYZED", item.path("riskLevel").asText());
            }
        }
        assertTrue(hasAnalyzed && hasUnanalyzed);
    }

    @Test
    void ut11ExportXlsx() throws Exception {
        Long group = createGroup("导出组").getId();
        Long project = insertProject(group, "导出项目", "APPROVED", "PLATFORM",
                LocalDate.of(2040, 1, 1), "1200000");
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        snapshot(run.getId(), project, group, LocalDate.of(2040, 6, 30), "80.00", "75.00", 1, 0, 0, 0, 0);

        MvcResult result = mockMvc.perform(get("/api/v1/insight/investment/export")
                        .param("groupId", String.valueOf(group))
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn();
        byte[] content = result.getResponse().getContentAsByteArray();
        assertTrue(content.length > 100);
        assertEquals((byte) 'P', content[0]);
        assertEquals((byte) 'K', content[1]);
        assertTrue(result.getResponse().getContentType().contains("spreadsheetml"));
    }

    @Test
    void ut11bExportCsv() throws Exception {
        Long group = createGroup("CSV导出组").getId();
        Long project = insertProject(group, "CSV项目", "APPROVED", "PLATFORM",
                LocalDate.of(2041, 1, 1), "900000");
        AnalysisRun run = insertRun("FULL", "SUCCESS", null);
        snapshot(run.getId(), project, group, LocalDate.of(2041, 6, 30), "70.00", "65.00", 0, 0, 0, 0, 0);

        MvcResult result = mockMvc.perform(get("/api/v1/insight/investment/export/csv")
                        .param("groupId", String.valueOf(group))
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn();
        byte[] content = result.getResponse().getContentAsByteArray();
        assertTrue(content.length > 100);
        assertEquals((byte) 0xEF, content[0]);
        assertEquals((byte) 0xBB, content[1]);
        assertEquals((byte) 0xBF, content[2]);
        assertTrue(result.getResponse().getContentType().contains("text/csv"));

        String text = new String(content, StandardCharsets.UTF_8);
        assertTrue(text.contains("[年度分布]"));
        assertTrue(text.contains("[技术方向分布]"));
        assertTrue(text.contains("2041"));
    }
}

package com.taiji.fund.insight.trend;

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
 * UT-17：CSV 导出 BOM、表头、口径与行数。
 */
class TrendCsvExportTest extends AbstractTrendTest {

    @Test
    void ut17ExportsCsvWithBomAndNote() throws Exception {
        Long group = intelGroupId();
        Long projectId = newProject(group, "导出项目");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectId, group, LocalDate.of(2022, 5, 10), "60.00", "50.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), projectId, group, LocalDate.of(2022, 6, 10), "70.00", "60.00", 1, 0, 0, 0, 0);

        MvcResult result = mockMvc.perform(get("/api/v1/insight/trend/export")
                        .param("startMonth", "2022-05")
                        .param("endMonth", "2022-06")
                        .header("Authorization", bearer(pmoToken())))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertTrue(content.length > 3);
        assertEquals((byte) 0xEF, content[0]);
        assertEquals((byte) 0xBB, content[1]);
        assertEquals((byte) 0xBF, content[2]);

        String text = new String(content, StandardCharsets.UTF_8);
        assertTrue(text.contains("period,analyzedProjectCount,avgMaxScore"));
        assertTrue(text.contains("uniquePairCount"));
        assertTrue(text.contains("2022-05"));
        assertTrue(text.contains("2022-06"));
        assertTrue(text.contains("口径"));
        assertTrue(text.contains("生成时间"));
        int dataRows = 0;
        for (String line : text.split("\r\n")) {
            if (line.startsWith("\"2022-")) {
                dataRows++;
            }
        }
        assertEquals(2, dataRows, "数据行数应与时间点一致");
    }
}

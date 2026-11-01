package com.taiji.fund.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.system.user.User;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-D06/D07：决策记录保存、查询、作废、权限与 Word/Excel 报告。
 */
class DecisionRecordReportTest extends AbstractDecisionTest {

    @Test
    @DisplayName("UT-D06 决策记录保存、组范围、专家只读、作废")
    void ut06RecordLifecycleAndScope() throws Exception {
        Long group = groupId("GRP-INTELLIGENCE");
        Long otherGroup = groupId("GRP-DATA");
        String text = uniqueText("记录流程");
        submittedProject(group, uniqueProjectName("记录对照"), text);
        long target = submittedProject(group, uniqueProjectName("记录目标"), text);
        String token = tokenOf(createGroupUser("decision-record", group));

        JsonNode precheck = precheck(token, "{\"mode\":\"PROJECT\",\"projectId\":" + target + "}");
        JsonNode record = saveDecision(token, precheck);
        long recordId = record.path("id").asLong();
        assertThat(recordId).isPositive();
        assertThat(record.path("similarProjects").size()).isGreaterThan(0);

        JsonNode page = records(token, "projectId=" + target);
        assertThat(page.path("total").asLong()).isEqualTo(1L);
        assertThat(recordDetail(token, recordId).path("recommendation").asText())
                .isEqualTo(precheck.path("recommendation").asText());

        String otherToken = tokenOf(createGroupUser("decision-other", otherGroup));
        assertThat(records(otherToken, "projectId=" + target).path("total").asLong()).isZero();
        mockMvc.perform(get("/api/v1/decision/records/{id}", recordId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());

        User expert = createUser("decision-reader", PASSWORD, UserStatus.ENABLED,
                group, RoleCode.EXPERT, 0);
        String expertToken = tokenOf(expert);
        recordDetail(expertToken, recordId);

        voidRecord(token, recordId, 403);
        voidRecord(pmoToken(), recordId, 200);
        mockMvc.perform(get("/api/v1/decision/records/{id}", recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        assertThat(records(token, "projectId=" + target).path("total").asLong()).isZero();
    }

    @Test
    @DisplayName("UT-D07 Word/Excel 报告可回读且章节完整")
    void ut07WordAndExcelReports() throws Exception {
        Long group = groupId("GRP-INTELLIGENCE");
        String text = uniqueText("报告生成");
        submittedProject(group, uniqueProjectName("报告对照"), text);
        long target = submittedProject(group, uniqueProjectName("报告目标"), text);
        String token = adminToken();

        JsonNode precheck = precheck(token, "{\"mode\":\"PROJECT\",\"projectId\":" + target + "}");
        JsonNode record = saveDecision(token, precheck);
        long recordId = record.path("id").asLong();
        Project project = projectMapper.selectById(target);

        MvcResult word = download(token, recordId, "docx");
        assertThat(word.getResponse().getHeader("Content-Disposition"))
                .contains(".docx").contains(project.getCode());
        String wordText = readDocx(word.getResponse().getContentAsByteArray());
        assertThat(wordText).contains("自投项目重复投入辅助决策报告");
        assertThat(wordText).contains("一、决策结论");
        assertThat(wordText).contains("二、Top-5 相似项目");
        assertThat(wordText).contains("三、重复点");
        assertThat(wordText).contains("四、差异点");
        assertThat(wordText).contains("五、口径与免责说明");
        assertThat(wordText).contains("六、大模型复核");
        assertThat(wordText).contains("本地算法结果");
        assertThat(wordText).contains(project.getCode());

        MvcResult excel = download(token, recordId, "xlsx");
        assertThat(excel.getResponse().getHeader("Content-Disposition"))
                .contains(".xlsx").contains(project.getCode());
        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(excel.getResponse().getContentAsByteArray()))) {
            assertThat(sheetNames(workbook)).contains("相似项目", "重复点", "差异点", "口径", "模型复核");
            assertThat(workbook.getSheet("相似项目").getRow(0).getCell(2).getStringCellValue())
                    .isEqualTo("项目名称");
        }
    }

    private String readDocx(byte[] content) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            document.getParagraphs().forEach(paragraph -> builder.append(paragraph.getText()).append('\n'));
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        builder.append(cell.getText()).append(' ');
                    }
                    builder.append('\n');
                }
            }
        }
        return builder.toString();
    }

    private List<String> sheetNames(XSSFWorkbook workbook) {
        List<String> names = new ArrayList<>();
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            names.add(workbook.getSheetName(index));
        }
        return names;
    }
}

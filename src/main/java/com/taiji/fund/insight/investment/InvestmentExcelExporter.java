package com.taiji.fund.insight.investment;

import com.taiji.fund.insight.investment.dto.DistributionItemVO;
import com.taiji.fund.insight.investment.dto.InvestmentOverviewVO;
import com.taiji.fund.insight.investment.dto.InvestmentProjectVO;
import com.taiji.fund.insight.investment.dto.InvestmentQuery;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 投入态势 Excel 导出：概览、年度、项目组、技术方向、Top 风险与口径说明。
 */
@Component
public class InvestmentExcelExporter {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InvestmentQueryService queryService;

    public InvestmentExcelExporter(InvestmentQueryService queryService) {
        this.queryService = queryService;
    }

    public byte[] export(InvestmentQuery query) {
        InvestmentOverviewVO overview = queryService.overview(query);
        List<DistributionItemVO> byYear = queryService.byYear(query);
        List<DistributionItemVO> byGroup = queryService.byGroup(query);
        List<DistributionItemVO> byDirection = queryService.byDirection(query);
        List<InvestmentProjectVO> topRisk = queryService.topRisk(query);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            overviewSheet(workbook, overview);
            distributionSheet(workbook, "年度分布", "年度", byYear);
            distributionSheet(workbook, "项目组分布", "项目组", byGroup);
            distributionSheet(workbook, "技术方向分布", "技术方向", byDirection);
            riskSheet(workbook, topRisk);
            noteSheet(workbook, overview);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("投入态势导出失败", exception);
        }
    }

    private void overviewSheet(SXSSFWorkbook workbook, InvestmentOverviewVO overview) {
        Sheet sheet = workbook.createSheet("概览");
        int rowIndex = 0;
        rowIndex = pair(sheet, rowIndex, "指标", "数值");
        rowIndex = pair(sheet, rowIndex, "投入总额（元）", text(overview.getTotalBudget()));
        rowIndex = pair(sheet, rowIndex, "项目数", String.valueOf(overview.getProjectCount()));
        rowIndex = pair(sheet, rowIndex, "在研项目数", String.valueOf(overview.getInProgressCount()));
        rowIndex = pair(sheet, rowIndex, "结项项目数", String.valueOf(overview.getCompletedCount()));
        rowIndex = pair(sheet, rowIndex, "平均预算（元）", text(overview.getAvgBudget()));
        rowIndex = pair(sheet, rowIndex, "已分析项目数", String.valueOf(overview.getAnalyzedCount()));
        rowIndex = pair(sheet, rowIndex, "未分析项目数", String.valueOf(overview.getUnanalyzedCount()));
        rowIndex = pair(sheet, rowIndex, "平均最高重复度", text(overview.getAvgMaxScore()));
        rowIndex = pair(sheet, rowIndex, "高风险项目数", String.valueOf(overview.getHighRiskCount()));
        rowIndex = pair(sheet, rowIndex, "风险敞口（元）", text(overview.getExposure()));
        rowIndex = pair(sheet, rowIndex, "高风险敞口（元）", text(overview.getHighExposure()));
        pair(sheet, rowIndex, "投入同比", overview.getYoyBudgetRatio() == null
                ? "—" : percent(overview.getYoyBudgetRatio()));
        width(sheet, 2);
    }

    private void distributionSheet(SXSSFWorkbook workbook, String name, String keyLabel,
                                   List<DistributionItemVO> items) {
        Sheet sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        String[] headers = {keyLabel, "项目数", "投入额（元）", "占比", "平均最高重复度", "高风险项目数", "风险敞口（元）"};
        for (int index = 0; index < headers.length; index++) {
            header.createCell(index).setCellValue(headers[index]);
        }
        int rowIndex = 1;
        for (DistributionItemVO item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getLabel());
            row.createCell(1).setCellValue(item.getProjectCount());
            row.createCell(2).setCellValue(text(item.getBudget()));
            row.createCell(3).setCellValue(percent(item.getBudgetShare()));
            row.createCell(4).setCellValue(text(item.getAvgMaxScore()));
            row.createCell(5).setCellValue(item.getHighRiskCount());
            row.createCell(6).setCellValue(text(item.getExposure()));
        }
        width(sheet, headers.length);
    }

    private void riskSheet(SXSSFWorkbook workbook, List<InvestmentProjectVO> projects) {
        Sheet sheet = workbook.createSheet("Top风险项目");
        String[] headers = {"项目编码", "项目名称", "项目组", "技术方向", "状态",
                "预算（元）", "最高重复度", "等级", "风险敞口（元）"};
        Row header = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            header.createCell(index).setCellValue(headers[index]);
        }
        int rowIndex = 1;
        for (InvestmentProjectVO project : projects) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(nullSafe(project.getCode()));
            row.createCell(1).setCellValue(nullSafe(project.getName()));
            row.createCell(2).setCellValue(nullSafe(project.getGroupName()));
            row.createCell(3).setCellValue(nullSafe(project.getTechDirectionName()));
            row.createCell(4).setCellValue(nullSafe(project.getStatus()));
            row.createCell(5).setCellValue(text(project.getBudget()));
            row.createCell(6).setCellValue(text(project.getMaxScore()));
            row.createCell(7).setCellValue(nullSafe(project.getRiskLevel()));
            row.createCell(8).setCellValue(text(project.getExposure()));
        }
        width(sheet, headers.length);
    }

    private void noteSheet(SXSSFWorkbook workbook, InvestmentOverviewVO overview) {
        Sheet sheet = workbook.createSheet("口径说明");
        int rowIndex = 0;
        rowIndex = pair(sheet, rowIndex, "生成时间",
                LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(TIME_FORMAT));
        rowIndex = pair(sheet, rowIndex, "口径", overview.getNote());
        rowIndex = pair(sheet, rowIndex, "高风险阈值",
                overview.getThresholds() == null ? "" : String.valueOf(overview.getThresholds().get("high")));
        pair(sheet, rowIndex, "未分析项目", "不计入平均分分母，不计算敞口");
        width(sheet, 2);
    }

    private int pair(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return rowIndex + 1;
    }

    private void width(Sheet sheet, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.setColumnWidth(index, 22 * 256);
        }
    }

    private String text(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String percent(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "%";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

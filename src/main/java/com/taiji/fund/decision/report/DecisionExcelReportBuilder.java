package com.taiji.fund.decision.report;

import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import com.taiji.fund.analysis.score.MatchPoint;
import com.taiji.fund.decision.dto.DecisionRecordVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Excel 决策报告：相似项目明细、分维度分、重复/差异点与口径。
 */
@Component
public class DecisionExcelReportBuilder {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] build(DecisionRecordVO record) {
        List<PeerMatchVO> peers = record.getSimilarProjects() == null
                ? Collections.emptyList() : record.getSimilarProjects();
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            peerSheet(workbook, peers);
            pointSheet(workbook, "重复点", peers, true);
            pointSheet(workbook, "差异点", peers, false);
            llmSheet(workbook, peers);
            noteSheet(workbook, record);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Excel 报告生成失败", exception);
        }
    }

    private void peerSheet(SXSSFWorkbook workbook, List<PeerMatchVO> peers) {
        Sheet sheet = workbook.createSheet("相似项目");
        String[] headers = {"序号", "项目编码", "项目名称", "项目组", "负责人", "状态", "预算（元）",
                "总分", "目标分", "研制内容分", "关键技术分", "节点分", "标签分", "归因"};
        header(sheet, headers);
        int rowIndex = 1;
        for (int index = 0; index < peers.size(); index++) {
            PeerMatchVO peer = peers.get(index);
            Row row = sheet.createRow(rowIndex++);
            int column = 0;
            row.createCell(column++).setCellValue(index + 1);
            row.createCell(column++).setCellValue(peer.getPeer() == null
                    ? "" : nullSafe(peer.getPeer().getCode()));
            row.createCell(column++).setCellValue(peer.getPeer() == null
                    ? "" : nullSafe(peer.getPeer().getName()));
            row.createCell(column++).setCellValue(peer.getPeer() == null
                    ? "" : nullSafe(peer.getPeer().getGroupName()));
            row.createCell(column++).setCellValue(peer.getPeer() == null
                    ? "" : nullSafe(peer.getPeer().getOwnerName()));
            row.createCell(column++).setCellValue(peer.getPeer() == null
                    ? "" : nullSafe(peer.getPeer().getStatus()));
            row.createCell(column++).setCellValue(peer.getPeer() == null || peer.getPeer().getBudget() == null
                    ? "" : peer.getPeer().getBudget().toPlainString());
            row.createCell(column++).setCellValue(text(peer.getTotalScore()));
            if (peer.getScores() != null) {
                row.createCell(column++).setCellValue(peer.getScores().getGoal());
                row.createCell(column++).setCellValue(peer.getScores().getContent());
                row.createCell(column++).setCellValue(peer.getScores().getTech());
                row.createCell(column++).setCellValue(peer.getScores().getMilestone());
                row.createCell(column++).setCellValue(peer.getScores().getTag());
            } else {
                for (int empty = 0; empty < 5; empty++) {
                    row.createCell(column++).setCellValue("");
                }
            }
            row.createCell(column).setCellValue(directionLabel(peer.getDirection()));
        }
        width(sheet, headers.length);
    }

    private void pointSheet(SXSSFWorkbook workbook, String name,
                            List<PeerMatchVO> peers, boolean matched) {
        Sheet sheet = workbook.createSheet(name);
        String[] headers = {"序号", "相似项目", "维度", "关键词", "原文片段"};
        header(sheet, headers);
        int rowIndex = 1;
        int sequence = 1;
        for (PeerMatchVO peer : peers) {
            List<MatchPoint> points = matched ? peer.getMatchedPoints() : peer.getDiffPoints();
            if (points == null) {
                continue;
            }
            for (MatchPoint point : points) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(sequence++);
                row.createCell(1).setCellValue(peer.getPeer() == null
                        ? "" : nullSafe(peer.getPeer().getName()));
                row.createCell(2).setCellValue(nullSafe(point.getDimension()));
                row.createCell(3).setCellValue(point.getTerms() == null
                        ? "" : String.join("、", point.getTerms()));
                row.createCell(4).setCellValue(nullSafe(point.getSnippet()));
            }
        }
        width(sheet, headers.length);
    }

    private void llmSheet(SXSSFWorkbook workbook, List<PeerMatchVO> peers) {
        Sheet sheet = workbook.createSheet("模型复核");
        String[] headers = {"序号", "项目名称", "模型结论", "模型重复点", "模型差异点"};
        header(sheet, headers);
        int rowIndex = 1;
        int sequence = 1;
        boolean has = false;
        for (PeerMatchVO peer : peers) {
            boolean hasReason = peer.getLlmReason() != null && !peer.getLlmReason().trim().isEmpty();
            boolean hasOverlaps = peer.getLlmOverlaps() != null && !peer.getLlmOverlaps().isEmpty();
            boolean hasDifferences = peer.getLlmDifferences() != null && !peer.getLlmDifferences().isEmpty();
            if (!hasReason && !hasOverlaps && !hasDifferences) {
                continue;
            }
            has = true;
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(sequence++);
            row.createCell(1).setCellValue(peer.getPeer() == null
                    ? "" : nullSafe(peer.getPeer().getName()));
            row.createCell(2).setCellValue(nullSafe(peer.getLlmReason()));
            row.createCell(3).setCellValue(join(peer.getLlmOverlaps()));
            row.createCell(4).setCellValue(join(peer.getLlmDifferences()));
        }
        if (!has) {
            Row row = sheet.createRow(rowIndex);
            row.createCell(1).setCellValue("本地算法结果");
            row.createCell(2).setCellValue("大模型未启用或调用失败");
        }
        width(sheet, headers.length);
    }

    private String join(List<String> values) {
        return values == null ? "" : String.join("；", values);
    }

    private void noteSheet(SXSSFWorkbook workbook, DecisionRecordVO record) {
        Sheet sheet = workbook.createSheet("口径");
        int rowIndex = 0;
        rowIndex = pair(sheet, rowIndex, "报告编号", "DR-" + record.getId());
        rowIndex = pair(sheet, rowIndex, "项目编码", nullSafe(record.getProjectCode()));
        rowIndex = pair(sheet, rowIndex, "风险等级", nullSafe(record.getLevel()));
        rowIndex = pair(sheet, rowIndex, "风险分", text(record.getRiskScore()));
        rowIndex = pair(sheet, rowIndex, "建议动作", nullSafe(record.getRecommendation()));
        rowIndex = pair(sheet, rowIndex, "预计重复投入（元）", text(record.getSavingsEstimate()));
        rowIndex = pair(sheet, rowIndex, "生成时间",
                LocalDateTime.now().format(TIME_FORMAT));
        pair(sheet, rowIndex, "口径",
                "本地算法（大模型未参与）；风险分取 Top-1 相似项目总分；"
                        + "预计重复投入 = 预算 × 风险分 / 100；结果仅供决策参考。");
        width(sheet, 2);
    }

    private int pair(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return rowIndex + 1;
    }

    private void header(Sheet sheet, String[] headers) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            row.createCell(index).setCellValue(headers[index]);
        }
    }

    private void width(Sheet sheet, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.setColumnWidth(index, 20 * 256);
        }
    }

    private String text(java.math.BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String directionLabel(String direction) {
        if ("SELF_GROUP".equals(direction)) {
            return "本组内部";
        }
        if ("PEER_EARLIER".equals(direction)) {
            return "他组在先";
        }
        if ("OUR_EARLIER".equals(direction)) {
            return "我方在先";
        }
        if ("SIMULTANEOUS".equals(direction)) {
            return "同期并行";
        }
        return nullSafe(direction);
    }
}

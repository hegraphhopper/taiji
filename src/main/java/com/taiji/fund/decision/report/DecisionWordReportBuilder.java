package com.taiji.fund.decision.report;

import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import com.taiji.fund.analysis.score.MatchPoint;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Word 决策报告：标题、元信息、结论、相似项目、重复/差异点与口径说明。
 */
@Component
@RequiredArgsConstructor
public class DecisionWordReportBuilder {

    private static final String FONT = "宋体";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] build(com.taiji.fund.decision.dto.DecisionRecordVO record) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            title(document);
            metaTable(document, record);
            conclusion(document, record);
            similarTable(document, record.getSimilarProjects());
            pointsTable(document, "三、重复点", record.getSimilarProjects(), true);
            pointsTable(document, "四、差异点", record.getSimilarProjects(), false);
            note(document);
            document.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Word 报告生成失败", exception);
        }
    }

    private void title(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(240);
        XWPFRun run = paragraph.createRun();
        run.setText("自投项目重复投入辅助决策报告");
        run.setBold(true);
        run.setFontSize(18);
        font(run);
    }

    private void metaTable(XWPFDocument document, com.taiji.fund.decision.dto.DecisionRecordVO record) {
        XWPFTable table = document.createTable(6, 2);
        table.setWidth("100%");
        pair(table, 0, "项目名称", value(record.getProjectName(), "临时文本预检"));
        pair(table, 1, "项目编码", value(record.getProjectCode(), "—"));
        pair(table, 2, "项目组", value(record.getGroupName(), "—"));
        pair(table, 3, "报告编号", "DR-" + record.getId());
        pair(table, 4, "操作人", value(record.getOperatorName(), "—"));
        pair(table, 5, "生成时间", LocalDateTime.now().format(TIME_FORMAT));
    }

    private void conclusion(XWPFDocument document, com.taiji.fund.decision.dto.DecisionRecordVO record) {
        heading(document, "一、决策结论");
        paragraph(document, "风险等级：" + value(record.getLevel(), "NONE")
                + "；风险分：" + score(record.getRiskScore()));
        paragraph(document, "建议动作：" + actionLabel(record.getRecommendation()));
        paragraph(document, "预计重复投入：" + money(record.getSavingsEstimate()));
        paragraph(document, "理由：" + value(record.getReason(), "—"));
    }

    private void similarTable(XWPFDocument document, List<PeerMatchVO> peers) {
        heading(document, "二、Top-5 相似项目");
        List<PeerMatchVO> rows = peers == null ? Collections.emptyList() : peers;
        XWPFTable table = document.createTable(rows.size() + 1, 7);
        table.setWidth("100%");
        String[] headers = {"序号", "项目名称", "项目组", "负责人", "状态", "总分", "归因"};
        header(table, headers);
        for (int index = 0; index < rows.size(); index++) {
            PeerMatchVO peer = rows.get(index);
            int row = index + 1;
            cell(table, row, 0, String.valueOf(index + 1));
            cell(table, row, 1, peer.getPeer() == null ? "—" : value(peer.getPeer().getName(), "—"));
            cell(table, row, 2, peer.getPeer() == null ? "—" : value(peer.getPeer().getGroupName(), "—"));
            cell(table, row, 3, peer.getPeer() == null ? "—" : value(peer.getPeer().getOwnerName(), "—"));
            cell(table, row, 4, peer.getPeer() == null ? "—" : value(peer.getPeer().getStatus(), "—"));
            cell(table, row, 5, score(peer.getTotalScore()));
            cell(table, row, 6, directionLabel(peer.getDirection()));
        }
    }

    private void pointsTable(XWPFDocument document, String heading,
                             List<PeerMatchVO> peers, boolean matched) {
        heading(document, heading);
        List<MatchPoint> points = matched
                ? collect(peers, true) : collect(peers, false);
        XWPFTable table = document.createTable(points.size() + 1, 4);
        table.setWidth("100%");
        String[] headers = {"序号", "维度", "关键词", "原文片段"};
        header(table, headers);
        for (int index = 0; index < points.size(); index++) {
            MatchPoint point = points.get(index);
            int row = index + 1;
            cell(table, row, 0, String.valueOf(index + 1));
            cell(table, row, 1, value(point.getDimension(), "—"));
            cell(table, row, 2, point.getTerms() == null
                    ? "—" : String.join("、", point.getTerms()));
            cell(table, row, 3, value(point.getSnippet(), "—"));
        }
    }

    private List<MatchPoint> collect(List<PeerMatchVO> peers, boolean matched) {
        if (peers == null || peers.isEmpty()) {
            return Collections.emptyList();
        }
        PeerMatchVO top = peers.get(0);
        List<MatchPoint> points = matched ? top.getMatchedPoints() : top.getDiffPoints();
        return points == null ? Collections.emptyList() : points;
    }

    private void note(XWPFDocument document) {
        heading(document, "五、口径与免责说明");
        paragraph(document, "本报告基于系统内结构化申报文本，采用本地算法（大模型未参与）计算，"
                + "相似度与建议动作仅供立项决策参考，最终结论由基金办评审确定。");
        paragraph(document, "风险分取 Top-1 相似项目总分；预计重复投入 = 预算 × 风险分 / 100。");
    }

    private void heading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(200);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(13);
        font(run);
    }

    private void paragraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(10.5);
        font(run);
    }

    private void pair(XWPFTable table, int rowIndex, String key, String value) {
        cell(table, rowIndex, 0, key);
        cell(table, rowIndex, 1, value);
    }

    private void header(XWPFTable table, String[] headers) {
        XWPFTableRow row = table.getRow(0);
        for (int index = 0; index < headers.length; index++) {
            XWPFTableCell cell = row.getCell(index);
            cell.removeParagraph(0);
            XWPFRun run = cell.addParagraph().createRun();
            run.setText(headers[index]);
            run.setBold(true);
            run.setFontSize(10);
            font(run);
        }
    }

    private void cell(XWPFTable table, int rowIndex, int cellIndex, String text) {
        XWPFTableRow row = table.getRow(rowIndex);
        XWPFTableCell cell = row.getCell(cellIndex);
        cell.removeParagraph(0);
        XWPFRun run = cell.addParagraph().createRun();
        run.setText(text);
        run.setFontSize(9.5);
        font(run);
    }

    private void font(XWPFRun run) {
        run.setFontFamily(FONT);
        run.setFontFamily(FONT, XWPFRun.FontCharRange.eastAsia);
    }

    private String score(BigDecimal value) {
        return value == null ? "—" : value.toPlainString();
    }

    private String money(BigDecimal value) {
        return value == null ? "—（缺少预算）" : value.toPlainString() + " 元";
    }

    private String actionLabel(String action) {
        if ("APPROVE".equals(action)) {
            return "APPROVE（可立项）";
        }
        if ("EXPLAIN".equals(action)) {
            return "EXPLAIN（补充差异说明）";
        }
        if ("MERGE".equals(action)) {
            return "MERGE（建议合并）";
        }
        if ("TERMINATE".equals(action)) {
            return "TERMINATE（建议终止/转向）";
        }
        return value(action, "—");
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
        return value(direction, "—");
    }

    private String value(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? fallback : text;
    }
}

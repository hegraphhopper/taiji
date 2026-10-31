package com.taiji.fund.insight.investment;

import com.taiji.fund.insight.investment.dto.DistributionItemVO;
import com.taiji.fund.insight.investment.dto.InvestmentOverviewVO;
import com.taiji.fund.insight.investment.dto.InvestmentQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 投入态势 CSV 导出：年度分布与技术方向分布两张表，附口径说明。
 */
@Component
public class InvestmentCsvExporter {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final String LINE = "\r\n";

    private final InvestmentQueryService queryService;

    public InvestmentCsvExporter(InvestmentQueryService queryService) {
        this.queryService = queryService;
    }

    public byte[] export(InvestmentQuery query) {
        InvestmentOverviewVO overview = queryService.overview(query);
        List<DistributionItemVO> byYear = queryService.byYear(query);
        List<DistributionItemVO> byDirection = queryService.byDirection(query);

        StringBuilder builder = new StringBuilder();
        builder.append("投入态势分析导出").append(LINE);
        builder.append("生成时间,").append(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(TIME_FORMAT))
                .append(LINE);
        builder.append("年度范围,").append(yearRange(query)).append(LINE);
        builder.append("含结项,").append(Boolean.FALSE.equals(query == null ? null : query.getIncludeCompleted())
                ? "否" : "是").append(LINE);
        builder.append(LINE);

        builder.append("[年度分布]").append(LINE);
        builder.append("年度,项目数,投入额(元),占比,平均最高重复度,高风险项目数,风险敞口(元)").append(LINE);
        for (DistributionItemVO item : byYear) {
            builder.append(cell(item.getLabel())).append(',')
                    .append(item.getProjectCount()).append(',')
                    .append(text(item.getBudget())).append(',')
                    .append(percent(item.getBudgetShare())).append(',')
                    .append(text(item.getAvgMaxScore())).append(',')
                    .append(item.getHighRiskCount()).append(',')
                    .append(text(item.getExposure())).append(LINE);
        }
        builder.append(LINE);

        builder.append("[技术方向分布]").append(LINE);
        builder.append("技术方向,项目数,投入额(元),占比,平均最高重复度,高风险项目数,风险敞口(元)").append(LINE);
        for (DistributionItemVO item : byDirection) {
            builder.append(cell(item.getLabel())).append(',')
                    .append(item.getProjectCount()).append(',')
                    .append(text(item.getBudget())).append(',')
                    .append(percent(item.getBudgetShare())).append(',')
                    .append(text(item.getAvgMaxScore())).append(',')
                    .append(item.getHighRiskCount()).append(',')
                    .append(text(item.getExposure())).append(LINE);
        }
        builder.append(LINE);

        builder.append("[口径说明]").append(LINE);
        builder.append("口径,").append(cell(overview.getNote())).append(LINE);
        builder.append("未分析项目,不计入平均分分母，不计算敞口").append(LINE);

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, content, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, content, UTF8_BOM.length, body.length);
        return content;
    }

    private String yearRange(InvestmentQuery query) {
        Integer start = query == null ? null : query.getStartYear();
        Integer end = query == null ? null : query.getEndYear();
        if (start == null && end == null) {
            return "全部";
        }
        return (start == null ? "不限" : String.valueOf(start))
                + " - " + (end == null ? "不限" : String.valueOf(end));
    }

    private String cell(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
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
}

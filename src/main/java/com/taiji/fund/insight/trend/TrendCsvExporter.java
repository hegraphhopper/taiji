package com.taiji.fund.insight.trend;

import com.taiji.fund.insight.trend.dto.AttributionCountsVO;
import com.taiji.fund.insight.trend.dto.TrendPointVO;
import com.taiji.fund.insight.trend.dto.TrendResponseVO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 趋势 CSV 导出：UTF-8 BOM + 口径说明，保证 Excel 中文正常。
 */
@Component
public class TrendCsvExporter {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(TrendResponseVO response) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("# 重复度趋势与归因导出\r\n");
        builder.append("# 生成时间：").append(LocalDateTime.now(TrendSupport.ZONE).format(TIME_FORMAT)).append("\r\n");
        builder.append("# 范围：").append(response.getStartMonth()).append(" ~ ").append(response.getEndMonth())
                .append("，粒度 ").append(response.getGranularity()).append("\r\n");
        builder.append("# 口径：").append(TrendSupport.DEFAULT_NOTE).append("\r\n");
        builder.append("period,analyzedProjectCount,avgMaxScore,avgTopScore,highRiskCount,pairCount,uniquePairCount,"
                + "highPairCount,selfGroup,peerEarlier,ourEarlier,simultaneous\r\n");
        if (response.getPoints() != null) {
            for (TrendPointVO point : response.getPoints()) {
                AttributionCountsVO counts = point.getAttribution();
                builder.append(csv(point.getPeriod())).append(',')
                        .append(point.getAnalyzedProjectCount()).append(',')
                        .append(text(point.getAvgMaxScore())).append(',')
                        .append(text(point.getAvgTopScore())).append(',')
                        .append(point.getHighRiskCount()).append(',')
                        .append(point.getPairCount()).append(',')
                        .append(point.getUniquePairCount()).append(',')
                        .append(point.getHighPairCount()).append(',')
                        .append(counts == null ? 0 : counts.getSelfGroup()).append(',')
                        .append(counts == null ? 0 : counts.getPeerEarlier()).append(',')
                        .append(counts == null ? 0 : counts.getOurEarlier()).append(',')
                        .append(counts == null ? 0 : counts.getSimultaneous()).append("\r\n");
            }
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}

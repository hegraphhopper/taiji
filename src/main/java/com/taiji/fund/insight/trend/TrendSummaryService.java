package com.taiji.fund.insight.trend;

import com.taiji.fund.insight.trend.dto.AttributionCountsVO;
import com.taiji.fund.insight.trend.dto.TrendPointVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 规则化结论摘要：数字全部取自趋势点，保证与图表一致。
 */
@Service
public class TrendSummaryService {

    private static final BigDecimal FLAT_THRESHOLD = BigDecimal.valueOf(1);

    public List<String> summarize(List<TrendPointVO> points, Map<String, Integer> groupHighRisk) {
        if (points == null || points.isEmpty()) {
            return Collections.singletonList("暂无可分析数据，建议先在「分析运行」触发一次全量分析。");
        }
        List<String> summary = new ArrayList<>();
        TrendPointVO last = points.get(points.size() - 1);
        if (points.size() < 2) {
            summary.add("当前仅有 1 个时间点（" + last.getPeriod() + "），数据不足以判断趋势。");
        } else {
            TrendPointVO previous = points.get(points.size() - 2);
            BigDecimal delta = last.getAvgMaxScore().subtract(previous.getAvgMaxScore());
            String direction;
            if (delta.compareTo(FLAT_THRESHOLD) > 0) {
                direction = "上升";
            } else if (delta.compareTo(FLAT_THRESHOLD.negate()) < 0) {
                direction = "下降";
            } else {
                direction = "基本持平";
            }
            String percent = "";
            if (previous.getAvgMaxScore().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = delta.abs()
                        .divide(previous.getAvgMaxScore(), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, BigDecimal.ROUND_HALF_UP);
                percent = "（" + ratio.toPlainString() + "%）";
            }
            summary.add("本期（" + last.getPeriod() + "）平均最高重复度 " + last.getAvgMaxScore().toPlainString()
                    + " 分，较上期" + direction + " " + delta.abs().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                    + " 分" + percent + "。");
        }

        AttributionCountsVO counts = last.getAttribution();
        if (counts == null || counts.total() == 0) {
            summary.add("本期未发现重复项目对。");
        } else {
            String main = mainAttribution(counts);
            BigDecimal ratio = mainRatio(last, main);
            summary.add("主要归因为" + label(main) + "，占 " + ratio.toPlainString() + "%。");
        }

        if (groupHighRisk != null && !groupHighRisk.isEmpty()) {
            List<String> groups = new ArrayList<>(groupHighRisk.keySet());
            summary.add("重点关注项目组：" + String.join("、", groups)
                    + "（高风险项目数居前）。");
        }

        summary.add("建议对高等级项目对发起差异说明或合并评估；本摘要仅给方向，不替代立项结论。");
        return summary;
    }

    private String mainAttribution(AttributionCountsVO counts) {
        int max = counts.getSelfGroup();
        String main = "selfGroup";
        if (counts.getPeerEarlier() > max) {
            max = counts.getPeerEarlier();
            main = "peerEarlier";
        }
        if (counts.getOurEarlier() > max) {
            max = counts.getOurEarlier();
            main = "ourEarlier";
        }
        if (counts.getSimultaneous() > max) {
            main = "simultaneous";
        }
        return main;
    }

    private BigDecimal mainRatio(TrendPointVO point, String main) {
        if (point.getRatios() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal ratio;
        switch (main) {
            case "peerEarlier":
                ratio = point.getRatios().getPeerEarlier();
                break;
            case "ourEarlier":
                ratio = point.getRatios().getOurEarlier();
                break;
            case "simultaneous":
                ratio = point.getRatios().getSimultaneous();
                break;
            case "selfGroup":
            default:
                ratio = point.getRatios().getSelfGroup();
                break;
        }
        if (ratio == null) {
            return BigDecimal.ZERO;
        }
        return ratio.multiply(BigDecimal.valueOf(100)).setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    private String label(String key) {
        switch (key) {
            case "peerEarlier":
                return "他组在先";
            case "ourEarlier":
                return "我方在先";
            case "simultaneous":
                return "同期并行";
            case "selfGroup":
            default:
                return "本组内部重复";
        }
    }
}

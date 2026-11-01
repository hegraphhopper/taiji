package com.taiji.fund.analysis.result.dto;

import com.taiji.fund.analysis.score.MatchPoint;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分析运行报告中的项目对：双方摘要、分维度得分、归因与可解释片段。
 *
 * <p>direction 保留落库的中性事实：SELF_GROUP / SIMULTANEOUS / A_EARLIER / B_EARLIER，
 * 避免在没有查看项目视角时产生「我方/他方」歧义。</p>
 */
@Data
@Builder
public class RunPairVO {

    private Long pairId;

    private AnalysisProjectBriefVO project;

    private AnalysisProjectBriefVO peer;

    private DimensionScoresVO scores;

    private BigDecimal localScore;

    private BigDecimal llmScore;

    private BigDecimal totalScore;

    private String level;

    private String relation;

    private String direction;

    private Long earlierProjectId;

    private List<MatchPoint> matchedPoints;

    private List<MatchPoint> diffPoints;

    private String llmReason;

    private List<String> llmOverlaps;

    private List<String> llmDifferences;
}

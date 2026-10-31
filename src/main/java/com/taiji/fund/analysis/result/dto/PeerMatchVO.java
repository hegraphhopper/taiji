package com.taiji.fund.analysis.result.dto;

import com.taiji.fund.analysis.score.MatchPoint;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Top-K 相似项目条目：分维度得分、融合分、等级、归因与可解释片段。
 *
 * <p>direction 已翻译为当前查看项目视角：OUR_EARLIER / PEER_EARLIER / SIMULTANEOUS / SELF_GROUP。</p>
 */
@Data
@Builder
public class PeerMatchVO {

    private Long pairId;

    private Long runId;

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

    /**
     * 建议动作，仅预检返回：HIGH→MERGE、MEDIUM/LOW→EXPLAIN、NONE→APPROVE。
     */
    private String suggestion;
}

package com.taiji.fund.analysis.result.dto;

import com.taiji.fund.analysis.score.MatchPoint;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目对明细：双方信息 + 分维度分 + 片段 + 大模型理由。
 */
@Data
@Builder
public class PairDetailVO {

    private Long pairId;

    private Long runId;

    private LocalDate analysisDate;

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

    private LocalDateTime createdAt;
}

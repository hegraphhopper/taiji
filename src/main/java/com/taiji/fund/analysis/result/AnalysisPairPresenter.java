package com.taiji.fund.analysis.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiji.fund.analysis.result.dto.AnalysisProjectBriefVO;
import com.taiji.fund.analysis.result.dto.DimensionScoresVO;
import com.taiji.fund.analysis.result.dto.PairDetailVO;
import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import com.taiji.fund.analysis.result.dto.RunPairVO;
import com.taiji.fund.analysis.score.DuplicateLevel;
import com.taiji.fund.analysis.score.MatchPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 分析结果展示转换：把落库的中性事实翻译为当前查看项目视角。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisPairPresenter {

    public static final String OUR_EARLIER = "OUR_EARLIER";
    public static final String PEER_EARLIER = "PEER_EARLIER";

    private static final TypeReference<List<MatchPoint>> POINT_LIST_TYPE =
            new TypeReference<List<MatchPoint>>() {
            };

    private static final TypeReference<List<String>> STRING_LIST_TYPE =
            new TypeReference<List<String>>() {
            };

    private final ObjectMapper objectMapper;

    /**
     * 中性 direction + earlierProjectId 翻译为查看项目视角。
     */
    public String viewDirection(AnalysisPair pair, Long viewerProjectId) {
        if (pair == null) {
            return Direction.SELF_GROUP.name();
        }
        String stored = pair.getDirection();
        if (Direction.SELF_GROUP.name().equals(stored)
                || Direction.SIMULTANEOUS.name().equals(stored)) {
            return stored;
        }
        Long earlier = pair.getEarlierProjectId();
        if (earlier == null || viewerProjectId == null) {
            return PEER_EARLIER;
        }
        return earlier.equals(viewerProjectId) ? OUR_EARLIER : PEER_EARLIER;
    }

    public Long viewPeerId(AnalysisPair pair, Long viewerProjectId) {
        if (pair == null) {
            return null;
        }
        if (viewerProjectId != null && viewerProjectId.equals(pair.getPeerProjectId())) {
            return pair.getProjectId();
        }
        return pair.getPeerProjectId();
    }

    public DimensionScoresVO scores(AnalysisPair pair) {
        return DimensionScoresVO.builder()
                .goal(toDouble(pair.getGoalScore()))
                .content(toDouble(pair.getContentScore()))
                .tech(toDouble(pair.getTechScore()))
                .milestone(toDouble(pair.getMilestoneScore()))
                .tag(toDouble(pair.getTagScore()))
                .build();
    }

    public PeerMatchVO toPeerMatch(AnalysisPair pair, Long viewerProjectId,
                                   AnalysisProjectBriefVO peer, String suggestion) {
        return PeerMatchVO.builder()
                .pairId(pair.getId())
                .runId(pair.getRunId())
                .peer(peer)
                .scores(scores(pair))
                .localScore(pair.getLocalScore())
                .llmScore(pair.getLlmScore())
                .totalScore(pair.getTotalScore())
                .level(pair.getLevel())
                .relation(pair.getRelation())
                .direction(viewDirection(pair, viewerProjectId))
                .earlierProjectId(pair.getEarlierProjectId())
                .matchedPoints(parsePoints(pair.getMatchedPoints()))
                .diffPoints(parsePoints(pair.getDiffPoints()))
                .llmReason(pair.getLlmReason())
                .llmOverlaps(parseStrings(pair.getLlmOverlaps()))
                .llmDifferences(parseStrings(pair.getLlmDifferences()))
                .suggestion(suggestion)
                .build();
    }

    public RunPairVO toRunPair(AnalysisPair pair, AnalysisProjectBriefVO project,
                               AnalysisProjectBriefVO peer) {
        return RunPairVO.builder()
                .pairId(pair.getId())
                .project(project)
                .peer(peer)
                .scores(scores(pair))
                .localScore(pair.getLocalScore())
                .llmScore(pair.getLlmScore())
                .totalScore(pair.getTotalScore())
                .level(pair.getLevel())
                .relation(pair.getRelation())
                .direction(pair.getDirection())
                .earlierProjectId(pair.getEarlierProjectId())
                .matchedPoints(parsePoints(pair.getMatchedPoints()))
                .diffPoints(parsePoints(pair.getDiffPoints()))
                .llmReason(pair.getLlmReason())
                .llmOverlaps(parseStrings(pair.getLlmOverlaps()))
                .llmDifferences(parseStrings(pair.getLlmDifferences()))
                .build();
    }

    public PairDetailVO toPairDetail(AnalysisPair pair, Long viewerProjectId,
                                     AnalysisProjectBriefVO project,
                                     AnalysisProjectBriefVO peer) {
        return PairDetailVO.builder()
                .pairId(pair.getId())
                .runId(pair.getRunId())
                .project(project)
                .peer(peer)
                .scores(scores(pair))
                .localScore(pair.getLocalScore())
                .llmScore(pair.getLlmScore())
                .totalScore(pair.getTotalScore())
                .level(pair.getLevel())
                .relation(pair.getRelation())
                .direction(viewDirection(pair, viewerProjectId))
                .earlierProjectId(pair.getEarlierProjectId())
                .matchedPoints(parsePoints(pair.getMatchedPoints()))
                .diffPoints(parsePoints(pair.getDiffPoints()))
                .llmReason(pair.getLlmReason())
                .llmOverlaps(parseStrings(pair.getLlmOverlaps()))
                .llmDifferences(parseStrings(pair.getLlmDifferences()))
                .createdAt(pair.getCreatedAt())
                .build();
    }

    /**
     * 按等级映射建议动作；预检返回，运行报告不返回。
     */
    public String suggestionOf(String level) {
        if (DuplicateLevel.HIGH.name().equals(level)) {
            return "MERGE";
        }
        if (DuplicateLevel.NONE.name().equals(level) || level == null) {
            return "APPROVE";
        }
        return "EXPLAIN";
    }

    public List<MatchPoint> parsePoints(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<MatchPoint> points = objectMapper.readValue(json, POINT_LIST_TYPE);
            return points == null ? new ArrayList<>() : points;
        } catch (JsonProcessingException exception) {
            log.warn("分析片段解析失败，按空列表处理: {}", exception.getClass().getSimpleName());
            return new ArrayList<>();
        }
    }

    public List<String> parseStrings(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST_TYPE);
            return values == null ? new ArrayList<>() : values;
        } catch (JsonProcessingException exception) {
            log.warn("LLM point parse failed: {}", exception.getClass().getSimpleName());
            return new ArrayList<>();
        }
    }

    private double toDouble(java.math.BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }
}

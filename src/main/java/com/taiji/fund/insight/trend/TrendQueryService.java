package com.taiji.fund.insight.trend;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.config.AnalysisConfigService;
import com.taiji.fund.analysis.config.AnalysisConfigVO;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.AnalysisPairMapper;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.insight.trend.dto.AttributionCountsVO;
import com.taiji.fund.insight.trend.dto.DrillProjectVO;
import com.taiji.fund.insight.trend.dto.TrendDrillQuery;
import com.taiji.fund.insight.trend.dto.TrendMomVO;
import com.taiji.fund.insight.trend.dto.TrendPointVO;
import com.taiji.fund.insight.trend.dto.TrendQuery;
import com.taiji.fund.insight.trend.dto.TrendResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 趋势聚合：月份去重、指标均值、四类归因汇总、环比与摘要。
 */
@Service
@RequiredArgsConstructor
public class TrendQueryService {

    private static final int TOP_GROUP_LIMIT = 2;

    private final TrendDataLoader dataLoader;
    private final AnalysisPairMapper pairMapper;
    private final AnalysisConfigService configService;
    private final TrendSummaryService summaryService;

    @Transactional(readOnly = true)
    public TrendResponseVO trend(TrendQuery query) {
        TrendScope scope = TrendScope.resolve(query);
        Granularity granularity = Granularity.parse(query.getGranularity());
        TrendData data = dataLoader.load(query, scope);
        List<SnapshotRow> rows = dataLoader.dedup(data.getRows(), granularity);

        Map<String, List<SnapshotRow>> byPeriod = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> TrendSupport.periodOf(row.getSnapshot().getAnalysisDate(), granularity),
                        TreeMap::new, Collectors.toList()));
        Map<String, Integer> uniquePairs = countUniquePairs(rows, granularity);

        List<TrendPointVO> points = new ArrayList<>(byPeriod.size());
        TrendPointVO previous = null;
        for (Map.Entry<String, List<SnapshotRow>> entry : byPeriod.entrySet()) {
            TrendPointVO point = buildPoint(entry.getKey(), entry.getValue(),
                    uniquePairs.getOrDefault(entry.getKey(), 0));
            point.setMom(momOf(previous, point));
            points.add(point);
            previous = point;
        }

        AnalysisConfigVO config = configService.current();
        Map<String, Integer> thresholds = config.getThresholds() == null
                ? Collections.<String, Integer>emptyMap() : config.getThresholds();
        Map<String, Integer> groupHighRisk = groupHighRisk(points.isEmpty() ? Collections.<SnapshotRow>emptyList()
                : byPeriod.get(points.get(points.size() - 1).getPeriod()));

        return TrendResponseVO.builder()
                .hasData(!points.isEmpty())
                .granularity(granularity.name())
                .startMonth(data.getRange().startLabel())
                .endMonth(data.getRange().endLabel())
                .points(points)
                .summary(summaryService.summarize(points, groupHighRisk))
                .thresholds(thresholds)
                .note(TrendSupport.DEFAULT_NOTE)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DrillProjectVO> drill(TrendDrillQuery query) {
        TrendScope scope = TrendScope.resolve(query);
        Granularity granularity = Granularity.parse(query.getGranularity());
        if (query.getPeriod() == null || query.getPeriod().trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "period 不能为空");
        }
        TrendData data = dataLoader.load(query, scope);
        List<SnapshotRow> rows = dataLoader.dedup(data.getRows(), granularity);
        AnalysisConfigVO config = configService.current();
        Map<String, Integer> thresholds = config.getThresholds() == null
                ? Collections.<String, Integer>emptyMap() : config.getThresholds();
        String period = query.getPeriod().trim();
        List<DrillProjectVO> projects = new ArrayList<>();
        for (SnapshotRow row : rows) {
            String rowPeriod = TrendSupport.periodOf(row.getSnapshot().getAnalysisDate(), granularity);
            if (!period.equals(rowPeriod)) {
                continue;
            }
            if (!matchesLevel(row, query.getLevel(), thresholds)) {
                continue;
            }
            projects.add(DrillProjectVO.builder()
                    .projectId(row.projectId())
                    .code(row.getProject() == null ? null : row.getProject().getCode())
                    .name(row.getProject() == null ? null : row.getProject().getName())
                    .groupId(row.getGroupId())
                    .groupName(row.getGroupName())
                    .techDirectionName(row.getTechDirectionName())
                    .fundType(row.fundType())
                    .maxScore(row.getSnapshot().getMaxScore())
                    .riskLevel(riskLevelOf(row, thresholds))
                    .attributionTotal(row.attributionTotal())
                    .analysisDate(row.getSnapshot().getAnalysisDate())
                    .build());
        }
        projects.sort((left, right) -> {
            BigDecimal leftScore = left.getMaxScore() == null ? BigDecimal.ZERO : left.getMaxScore();
            BigDecimal rightScore = right.getMaxScore() == null ? BigDecimal.ZERO : right.getMaxScore();
            return rightScore.compareTo(leftScore);
        });
        return projects;
    }

    /**
     * 唯一项目对：选定运行中按无序对去重后计数。
     */
    Map<String, Integer> countUniquePairs(List<SnapshotRow> rows, Granularity granularity) {
        Map<Long, String> runPeriod = new HashMap<>();
        for (SnapshotRow row : rows) {
            Long runId = row.getSnapshot().getRunId();
            if (runId != null) {
                runPeriod.put(runId, TrendSupport.periodOf(row.getSnapshot().getAnalysisDate(), granularity));
            }
        }
        if (runPeriod.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AnalysisPair> pairs = pairMapper.selectList(Wrappers.<AnalysisPair>lambdaQuery()
                .in(AnalysisPair::getRunId, runPeriod.keySet()));
        Map<String, Set<String>> sets = new HashMap<>();
        for (AnalysisPair pair : pairs) {
            String period = runPeriod.get(pair.getRunId());
            if (period == null || pair.getProjectId() == null || pair.getPeerProjectId() == null) {
                continue;
            }
            long left = Math.min(pair.getProjectId(), pair.getPeerProjectId());
            long right = Math.max(pair.getProjectId(), pair.getPeerProjectId());
            sets.computeIfAbsent(period, key -> new HashSet<>()).add(left + "-" + right);
        }
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : sets.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    private TrendPointVO buildPoint(String period, List<SnapshotRow> rows, int uniquePairCount) {
        double maxSum = 0D;
        double topSum = 0D;
        int highRisk = 0;
        int pairCount = 0;
        int highPairCount = 0;
        int selfGroup = 0;
        int peerEarlier = 0;
        int ourEarlier = 0;
        int simultaneous = 0;
        for (SnapshotRow row : rows) {
            maxSum += row.getSnapshot().getMaxScore() == null ? 0D
                    : row.getSnapshot().getMaxScore().doubleValue();
            topSum += row.getSnapshot().getAvgTopScore() == null ? 0D
                    : row.getSnapshot().getAvgTopScore().doubleValue();
            if (row.highPair() > 0) {
                highRisk++;
            }
            pairCount += row.attributionTotal();
            highPairCount += row.highPair();
            selfGroup += row.selfGroup();
            peerEarlier += row.peerEarlier();
            ourEarlier += row.ourEarlier();
            simultaneous += row.simultaneous();
        }
        int size = rows.size();
        AttributionCountsVO counts = AttributionCountsVO.builder()
                .selfGroup(selfGroup)
                .peerEarlier(peerEarlier)
                .ourEarlier(ourEarlier)
                .simultaneous(simultaneous)
                .build();
        return TrendPointVO.builder()
                .period(period)
                .analyzedProjectCount(size)
                .avgMaxScore(TrendSupport.scale(size == 0 ? 0D : maxSum / size))
                .avgTopScore(TrendSupport.scale(size == 0 ? 0D : topSum / size))
                .highRiskCount(highRisk)
                .pairCount(pairCount)
                .uniquePairCount(uniquePairCount)
                .highPairCount(highPairCount)
                .attribution(counts)
                .ratios(counts.ratios())
                .build();
    }

    private TrendMomVO momOf(TrendPointVO previous, TrendPointVO current) {
        if (previous == null) {
            return null;
        }
        BigDecimal delta = current.getAvgMaxScore().subtract(previous.getAvgMaxScore());
        return TrendMomVO.builder()
                .avgMaxScoreDelta(delta.setScale(2, RoundingMode.HALF_UP))
                .avgMaxScoreRatio(TrendSupport.ratio(delta, previous.getAvgMaxScore()))
                .build();
    }

    private Map<String, Integer> groupHighRisk(List<SnapshotRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SnapshotRow row : rows) {
            String name = row.getGroupName() == null ? "未归属项目组" : row.getGroupName();
            int value = counts.getOrDefault(name, 0);
            if (row.highPair() > 0) {
                value++;
            }
            counts.put(name, value);
        }
        return counts.entrySet().stream()
                .sorted((left, right) -> right.getValue().compareTo(left.getValue()))
                .limit(TOP_GROUP_LIMIT)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private boolean matchesLevel(SnapshotRow row, String level, Map<String, Integer> thresholds) {
        if (level == null || level.trim().isEmpty()) {
            return true;
        }
        String normalized = level.trim().toUpperCase();
        BigDecimal max = row.getSnapshot().getMaxScore() == null
                ? BigDecimal.ZERO : row.getSnapshot().getMaxScore();
        int low = threshold(thresholds, "low", 40);
        int medium = threshold(thresholds, "medium", 60);
        int high = threshold(thresholds, "high", 80);
        boolean highPair = row.highPair() > 0;
        switch (normalized) {
            case "HIGH":
                return highPair;
            case "MEDIUM":
                return !highPair && max.compareTo(BigDecimal.valueOf(medium)) >= 0;
            case "LOW":
                return !highPair && max.compareTo(BigDecimal.valueOf(medium)) < 0
                        && max.compareTo(BigDecimal.valueOf(low)) >= 0;
            case "NONE":
                return max.compareTo(BigDecimal.valueOf(low)) < 0;
            default:
                throw new BizException(ErrorCode.BAD_REQUEST, "level 仅支持 HIGH/MEDIUM/LOW/NONE");
        }
    }

    private int threshold(Map<String, Integer> thresholds, String key, int fallback) {
        Integer value = thresholds.get(key);
        return value == null ? fallback : value;
    }

    private String riskLevelOf(SnapshotRow row, Map<String, Integer> thresholds) {
        if (row.highPair() > 0) {
            return "HIGH";
        }
        BigDecimal max = row.getSnapshot().getMaxScore() == null
                ? BigDecimal.ZERO : row.getSnapshot().getMaxScore();
        if (max.compareTo(BigDecimal.valueOf(threshold(thresholds, "medium", 60))) >= 0) {
            return "MEDIUM";
        }
        if (max.compareTo(BigDecimal.valueOf(threshold(thresholds, "low", 40))) >= 0) {
            return "LOW";
        }
        return "NONE";
    }

    /**
     * 供导出复用：确保导出与页面使用同一份数据。
     */
    @Transactional(readOnly = true)
    public TrendResponseVO exportSource(TrendQuery query) {
        return trend(query);
    }
}

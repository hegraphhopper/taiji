package com.taiji.fund.insight.investment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.config.AnalysisConfigService;
import com.taiji.fund.analysis.config.AnalysisConfigVO;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.DuplicateSnapshotMapper;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.insight.investment.dto.DistributionItemVO;
import com.taiji.fund.insight.investment.dto.InvestmentOverviewVO;
import com.taiji.fund.insight.investment.dto.InvestmentProjectVO;
import com.taiji.fund.insight.investment.dto.InvestmentQuery;
import com.taiji.fund.insight.trend.TrendScope;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 技术创新投入态势：只读聚合项目预算、状态与最新重复度快照。
 */
@Service
@RequiredArgsConstructor
public class InvestmentQueryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final List<String> BASE_STATUSES = Arrays.asList(
            ProjectStatus.APPROVED.getCode(),
            ProjectStatus.IN_PROGRESS.getCode());

    private static final String NOTE = "金额单位为元；重复度取项目最新快照；未分析项目单列且不计入平均分分母；"
            + "高风险按当前阈值判定；敞口 = 预算 × 最高分 / 100。";

    private final ProjectMapper projectMapper;
    private final DuplicateSnapshotMapper snapshotMapper;
    private final GroupMapper groupMapper;
    private final TechDirectionMapper techDirectionMapper;
    private final AnalysisConfigService configService;

    @Transactional(readOnly = true)
    public InvestmentOverviewVO overview(InvestmentQuery query) {
        Loaded loaded = load(query);
        List<Row> rows = loaded.rows;
        BigDecimal totalBudget = sumBudget(rows);
        int analyzed = (int) rows.stream().filter(row -> row.analyzed).count();
        int highRisk = (int) rows.stream().filter(row -> row.analyzed && row.highRisk).count();
        BigDecimal avgScore = averageScore(rows);
        BigDecimal exposure = sumExposure(rows);
        BigDecimal highExposure = rows.stream()
                .filter(row -> row.analyzed && row.highRisk && row.exposure != null)
                .map(row -> row.exposure)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Map<Integer, List<Row>> byYear = rows.stream()
                .filter(row -> row.year != null)
                .collect(Collectors.groupingBy(row -> row.year, TreeMap::new, Collectors.toList()));
        BigDecimal yoyBudget = null;
        BigDecimal yoyCount = null;
        if (!byYear.isEmpty()) {
            int latestYear = byYear.keySet().stream().max(Integer::compareTo).orElse(0);
            List<Row> previous = byYear.get(latestYear - 1);
            if (previous != null && !previous.isEmpty()) {
                BigDecimal previousBudget = sumBudget(previous);
                BigDecimal latestBudget = sumBudget(byYear.get(latestYear));
                yoyBudget = ratio(latestBudget.subtract(previousBudget), previousBudget);
                yoyCount = ratio(BigDecimal.valueOf(byYear.get(latestYear).size() - previous.size()),
                        BigDecimal.valueOf(previous.size()));
            }
        }

        return InvestmentOverviewVO.builder()
                .hasData(!rows.isEmpty())
                .totalBudget(totalBudget)
                .projectCount(rows.size())
                .inProgressCount((int) rows.stream()
                        .filter(row -> ProjectStatus.IN_PROGRESS.getCode().equals(row.project.getStatus()))
                        .count())
                .completedCount((int) rows.stream()
                        .filter(row -> ProjectStatus.COMPLETED.getCode().equals(row.project.getStatus()))
                        .count())
                .avgBudget(rows.isEmpty() ? zero() : totalBudget.divide(
                        BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP))
                .analyzedCount(analyzed)
                .unanalyzedCount(rows.size() - analyzed)
                .avgMaxScore(avgScore)
                .highRiskCount(highRisk)
                .exposure(exposure)
                .highExposure(highExposure)
                .yoyBudgetRatio(yoyBudget)
                .yoyCountRatio(yoyCount)
                .thresholds(loaded.thresholds)
                .note(NOTE)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DistributionItemVO> byYear(InvestmentQuery query) {
        Loaded loaded = load(query);
        Map<Integer, List<Row>> grouped = loaded.rows.stream()
                .filter(row -> row.year != null)
                .collect(Collectors.groupingBy(row -> row.year, TreeMap::new, Collectors.toList()));
        List<DistributionItemVO> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Row>> entry : grouped.entrySet()) {
            result.add(distribution(String.valueOf(entry.getKey()), entry.getKey() + " 年",
                    entry.getValue(), loaded.totalBudget));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DistributionItemVO> byFundType(InvestmentQuery query) {
        Loaded loaded = load(query);
        Map<String, List<Row>> grouped = loaded.rows.stream()
                .collect(Collectors.groupingBy(row -> row.project.getFundType() == null
                        ? "OTHER" : row.project.getFundType(), LinkedHashMap::new, Collectors.toList()));
        List<DistributionItemVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Row>> entry : grouped.entrySet()) {
            result.add(distribution(entry.getKey(), fundTypeLabel(entry.getKey()),
                    entry.getValue(), loaded.totalBudget));
        }
        result.sort(Comparator.comparing(DistributionItemVO::getBudget).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public List<DistributionItemVO> byGroup(InvestmentQuery query) {
        Loaded loaded = load(query);
        Map<String, List<Row>> grouped = loaded.rows.stream()
                .collect(Collectors.groupingBy(row -> row.groupId == null ? "none" : row.groupId.toString(),
                        LinkedHashMap::new, Collectors.toList()));
        List<DistributionItemVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Row>> entry : grouped.entrySet()) {
            String label = entry.getValue().get(0).groupName;
            result.add(distribution(entry.getKey(), label == null ? "未归属项目组" : label,
                    entry.getValue(), loaded.totalBudget));
        }
        result.sort(Comparator.comparing(DistributionItemVO::getBudget).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public List<DistributionItemVO> byDirection(InvestmentQuery query) {
        Loaded loaded = load(query);
        Map<String, List<Row>> grouped = loaded.rows.stream()
                .collect(Collectors.groupingBy(row -> row.directionName == null ? "none" : row.directionName,
                        LinkedHashMap::new, Collectors.toList()));
        List<DistributionItemVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Row>> entry : grouped.entrySet()) {
            result.add(distribution(entry.getKey(), entry.getKey().equals("none") ? "未选方向" : entry.getKey(),
                    entry.getValue(), loaded.totalBudget));
        }
        result.sort(Comparator.comparing(DistributionItemVO::getBudget).reversed());
        return result.size() > 10 ? new ArrayList<>(result.subList(0, 10)) : result;
    }

    @Transactional(readOnly = true)
    public List<InvestmentProjectVO> topRisk(InvestmentQuery query) {
        Loaded loaded = load(query);
        return loaded.rows.stream()
                .filter(row -> row.analyzed && row.exposure != null)
                .sorted(Comparator.comparing((Row row) -> row.exposure).reversed())
                .limit(10)
                .map(row -> toVO(row, loaded.thresholds))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvestmentProjectVO> projects(InvestmentQuery query) {
        Loaded loaded = load(query);
        return loaded.rows.stream()
                .sorted(Comparator.comparing((Row row) -> row.exposure == null ? BigDecimal.valueOf(-1) : row.exposure)
                        .reversed()
                        .thenComparing(row -> row.project.getBudget() == null
                                ? BigDecimal.ZERO : row.project.getBudget(), Comparator.reverseOrder()))
                .map(row -> toVO(row, loaded.thresholds))
                .collect(Collectors.toList());
    }

    public Loaded loadForExport(InvestmentQuery query) {
        return load(query);
    }

    private Loaded load(InvestmentQuery query) {
        InvestmentQuery actual = query == null ? new InvestmentQuery() : query;
        TrendScope scope = TrendScope.resolve(actual.getGroupId());
        List<String> statuses = new ArrayList<>(BASE_STATUSES);
        if (!Boolean.FALSE.equals(actual.getIncludeCompleted())) {
            statuses.add(ProjectStatus.COMPLETED.getCode());
        }
        List<Project> projects = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                .in(Project::getStatus, statuses)
                .orderByAsc(Project::getId));
        Map<Long, String> groupNames = groupMapper.selectList(Wrappers.<Group>lambdaQuery())
                .stream().collect(Collectors.toMap(Group::getId, Group::getName, (left, right) -> left));
        Map<Long, String> directionNames = techDirectionMapper.selectList(Wrappers.<TechDirection>lambdaQuery())
                .stream().collect(Collectors.toMap(TechDirection::getId, TechDirection::getName,
                        (left, right) -> left));
        Map<String, Integer> thresholds = thresholds();

        List<Project> filtered = new ArrayList<>();
        for (Project project : projects) {
            if (scope.getGroupFilter() != null && !scope.getGroupFilter().equals(project.getGroupId())) {
                continue;
            }
            if (actual.getTechDirectionId() != null
                    && !actual.getTechDirectionId().equals(project.getTechDirectionId())) {
                continue;
            }
            if (actual.getFundType() != null && !actual.getFundType().trim().isEmpty()
                    && !actual.getFundType().equals(project.getFundType())) {
                continue;
            }
            Integer year = yearOf(project);
            if (actual.getStartYear() != null && (year == null || year < actual.getStartYear())) {
                continue;
            }
            if (actual.getEndYear() != null && (year == null || year > actual.getEndYear())) {
                continue;
            }
            filtered.add(project);
        }

        Map<Long, DuplicateSnapshot> latest = latestSnapshots(filtered);
        List<Row> rows = new ArrayList<>();
        for (Project project : filtered) {
            DuplicateSnapshot snapshot = latest.get(project.getId());
            Row row = new Row();
            row.project = project;
            row.snapshot = snapshot;
            row.groupId = project.getGroupId();
            row.groupName = project.getGroupId() == null ? null : groupNames.get(project.getGroupId());
            row.directionName = project.getTechDirectionId() == null ? null
                    : directionNames.get(project.getTechDirectionId());
            row.year = yearOf(project);
            row.analyzed = snapshot != null && snapshot.getMaxScore() != null;
            row.maxScore = row.analyzed ? snapshot.getMaxScore() : null;
            row.highRisk = row.analyzed && row.maxScore.compareTo(
                    BigDecimal.valueOf(threshold(thresholds, "high", 80))) >= 0;
            row.exposure = row.analyzed && project.getBudget() != null
                    ? project.getBudget().multiply(row.maxScore)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : null;
            rows.add(row);
        }
        return new Loaded(rows, thresholds, sumBudget(rows));
    }

    private Map<Long, DuplicateSnapshot> latestSnapshots(List<Project> projects) {
        List<Long> ids = projects.stream().map(Project::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DuplicateSnapshot> snapshots = snapshotMapper.selectList(Wrappers.<DuplicateSnapshot>lambdaQuery()
                .in(DuplicateSnapshot::getProjectId, ids)
                .orderByAsc(DuplicateSnapshot::getAnalysisDate)
                .orderByAsc(DuplicateSnapshot::getRunId));
        Map<Long, DuplicateSnapshot> latest = new HashMap<>();
        for (DuplicateSnapshot snapshot : snapshots) {
            latest.put(snapshot.getProjectId(), snapshot);
        }
        return latest;
    }

    private DistributionItemVO distribution(String key, String label, List<Row> rows, BigDecimal totalBudget) {
        BigDecimal budget = sumBudget(rows);
        BigDecimal share = totalBudget == null || totalBudget.compareTo(BigDecimal.ZERO) == 0
                ? zero() : budget.divide(totalBudget, 4, RoundingMode.HALF_UP);
        return DistributionItemVO.builder()
                .key(key)
                .label(label)
                .projectCount(rows.size())
                .budget(budget)
                .budgetShare(share)
                .avgMaxScore(averageScore(rows))
                .highRiskCount((int) rows.stream().filter(row -> row.analyzed && row.highRisk).count())
                .exposure(sumExposure(rows))
                .build();
    }

    private InvestmentProjectVO toVO(Row row, Map<String, Integer> thresholds) {
        return InvestmentProjectVO.builder()
                .projectId(row.project.getId())
                .code(row.project.getCode())
                .name(row.project.getName())
                .groupId(row.groupId)
                .groupName(row.groupName)
                .techDirectionName(row.directionName)
                .fundType(row.project.getFundType())
                .status(row.project.getStatus())
                .budget(row.project.getBudget())
                .maxScore(row.maxScore)
                .riskLevel(riskLevel(row, thresholds))
                .exposure(row.exposure)
                .year(row.year)
                .build();
    }

    private String riskLevel(Row row, Map<String, Integer> thresholds) {
        if (!row.analyzed) {
            return "UNANALYZED";
        }
        if (row.maxScore.compareTo(BigDecimal.valueOf(threshold(thresholds, "high", 80))) >= 0) {
            return "HIGH";
        }
        if (row.maxScore.compareTo(BigDecimal.valueOf(threshold(thresholds, "medium", 60))) >= 0) {
            return "MEDIUM";
        }
        if (row.maxScore.compareTo(BigDecimal.valueOf(threshold(thresholds, "low", 40))) >= 0) {
            return "LOW";
        }
        return "NONE";
    }

    private BigDecimal averageScore(List<Row> rows) {
        List<Row> analyzed = rows.stream().filter(row -> row.analyzed).collect(Collectors.toList());
        if (analyzed.isEmpty()) {
            return zero();
        }
        BigDecimal total = analyzed.stream().map(row -> row.maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(analyzed.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumBudget(List<Row> rows) {
        return rows.stream()
                .map(row -> row.project.getBudget() == null ? BigDecimal.ZERO : row.project.getBudget())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumExposure(List<Row> rows) {
        return rows.stream()
                .filter(row -> row.exposure != null)
                .map(row -> row.exposure)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP);
    }

    private Integer yearOf(Project project) {
        if (project.getStartDate() != null) {
            return project.getStartDate().getYear();
        }
        if (project.getCreatedAt() != null) {
            return project.getCreatedAt().atZone(ZONE).getYear();
        }
        return null;
    }

    private Map<String, Integer> thresholds() {
        AnalysisConfigVO config = configService.current();
        return config.getThresholds() == null
                ? Collections.<String, Integer>emptyMap() : config.getThresholds();
    }

    private int threshold(Map<String, Integer> thresholds, String key, int fallback) {
        Integer value = thresholds.get(key);
        return value == null ? fallback : value;
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String fundTypeLabel(String code) {
        if ("PLATFORM".equals(code)) {
            return "平台基金";
        }
        if ("INNOVATION".equals(code)) {
            return "创新基金";
        }
        return "其他";
    }

    static class Loaded {
        final List<Row> rows;
        final Map<String, Integer> thresholds;
        final BigDecimal totalBudget;

        Loaded(List<Row> rows, Map<String, Integer> thresholds, BigDecimal totalBudget) {
            this.rows = rows;
            this.thresholds = thresholds;
            this.totalBudget = totalBudget;
        }
    }

    static class Row {
        Project project;
        DuplicateSnapshot snapshot;
        Long groupId;
        String groupName;
        String directionName;
        Integer year;
        boolean analyzed;
        boolean highRisk;
        BigDecimal maxScore;
        BigDecimal exposure;
    }
}

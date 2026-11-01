package com.taiji.fund.dashboard;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.config.AnalysisConfigService;
import com.taiji.fund.analysis.config.AnalysisConfigVO;
import com.taiji.fund.analysis.config.Thresholds;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.DuplicateSnapshotMapper;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台汇总：项目总数、已分析数、平均最高重复度、高风险项目数。
 *
 * <p>项目范围复用 {@link ProjectAccessService#canView}；平均分与高风险取每个项目
 * 最新一条快照（analysisDate、runId 最大），与趋势/投入口径一致。</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private static final int DEFAULT_HIGH = 80;

    private final ProjectMapper projectMapper;
    private final DuplicateSnapshotMapper snapshotMapper;
    private final AnalysisConfigService configService;
    private final ProjectAccessService accessService;

    @Transactional(readOnly = true)
    public DashboardSummaryVO summary() {
        Map<String, Integer> thresholds = resolveThresholds();
        int highThreshold = thresholds.getOrDefault("high", DEFAULT_HIGH);

        List<Project> visible = new ArrayList<>();
        for (Project project : projectMapper.selectList(
                Wrappers.<Project>lambdaQuery().orderByAsc(Project::getId))) {
            if (accessService.canView(project)) {
                visible.add(project);
            }
        }

        Map<Long, DuplicateSnapshot> latest = latestSnapshots(visible);
        int analyzed = 0;
        int highRisk = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Project project : visible) {
            DuplicateSnapshot snapshot = latest.get(project.getId());
            if (snapshot == null || snapshot.getMaxScore() == null) {
                continue;
            }
            analyzed++;
            total = total.add(snapshot.getMaxScore());
            if (snapshot.getMaxScore().compareTo(BigDecimal.valueOf(highThreshold)) >= 0) {
                highRisk++;
            }
        }
        BigDecimal average = analyzed == 0 ? null
                : total.divide(BigDecimal.valueOf(analyzed), 2, RoundingMode.HALF_UP);

        return DashboardSummaryVO.builder()
                .projectCount(visible.size())
                .analyzedCount(analyzed)
                .avgMaxScore(average)
                .highRiskCount(highRisk)
                .thresholds(thresholds)
                .build();
    }

    private Map<Long, DuplicateSnapshot> latestSnapshots(List<Project> projects) {
        if (projects.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = new ArrayList<>(projects.size());
        for (Project project : projects) {
            ids.add(project.getId());
        }
        List<DuplicateSnapshot> snapshots = snapshotMapper.selectList(
                Wrappers.<DuplicateSnapshot>lambdaQuery()
                        .in(DuplicateSnapshot::getProjectId, ids)
                        .orderByAsc(DuplicateSnapshot::getAnalysisDate)
                        .orderByAsc(DuplicateSnapshot::getRunId));
        Map<Long, DuplicateSnapshot> latest = new HashMap<>();
        for (DuplicateSnapshot snapshot : snapshots) {
            latest.put(snapshot.getProjectId(), snapshot);
        }
        return latest;
    }

    private Map<String, Integer> resolveThresholds() {
        try {
            AnalysisConfigVO config = configService.current();
            Map<String, Integer> values = config.getThresholds();
            if (Thresholds.containsAllKeys(values) && Thresholds.of(values).isValid()) {
                return values;
            }
        } catch (RuntimeException ignored) {
            // 配置读取失败时使用默认阈值，不影响工作台展示
        }
        return Thresholds.defaults().toMap();
    }
}

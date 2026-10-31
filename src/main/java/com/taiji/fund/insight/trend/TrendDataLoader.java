package com.taiji.fund.insight.trend;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.DuplicateSnapshotMapper;
import com.taiji.fund.insight.trend.dto.TrendQuery;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 趋势数据载入：范围校验、快照加载、项目维度补全、过滤与月份去重。
 */
@Component
@RequiredArgsConstructor
public class TrendDataLoader {

    private final DuplicateSnapshotMapper snapshotMapper;
    private final ProjectMapper projectMapper;
    private final GroupMapper groupMapper;
    private final TechDirectionMapper techDirectionMapper;

    public TrendData load(TrendQuery query, TrendScope scope) {
        DateRange range = TrendSupport.resolveRange(query.getStartMonth(), query.getEndMonth());
        List<DuplicateSnapshot> snapshots = snapshotMapper.selectList(
                Wrappers.<DuplicateSnapshot>lambdaQuery()
                        .ge(DuplicateSnapshot::getAnalysisDate, range.startDate())
                        .le(DuplicateSnapshot::getAnalysisDate, range.endDate())
                        .orderByAsc(DuplicateSnapshot::getAnalysisDate)
                        .orderByAsc(DuplicateSnapshot::getProjectId));
        if (snapshots.isEmpty()) {
            return new TrendData(range, new ArrayList<>());
        }
        Set<Long> projectIds = snapshots.stream()
                .map(DuplicateSnapshot::getProjectId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Project> projects = projectIds.isEmpty() ? new HashMap<>()
                : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, project -> project, (left, right) -> left,
                        HashMap::new));

        Map<Long, String> groupNames = loadGroupNames();
        Map<Long, String> directionNames = loadDirectionNames();

        List<SnapshotRow> rows = new ArrayList<>(snapshots.size());
        for (DuplicateSnapshot snapshot : snapshots) {
            if (snapshot.getAnalysisDate() == null) {
                continue;
            }
            Project project = projects.get(snapshot.getProjectId());
            Long groupId = snapshot.getGroupId() != null ? snapshot.getGroupId()
                    : (project == null ? null : project.getGroupId());
            if (scope.getGroupFilter() != null && !scope.getGroupFilter().equals(groupId)) {
                continue;
            }
            if (query.getTechDirectionId() != null) {
                if (project == null || !query.getTechDirectionId().equals(project.getTechDirectionId())) {
                    continue;
                }
            }
            if (query.getFundType() != null && !query.getFundType().trim().isEmpty()) {
                if (project == null || !query.getFundType().equals(project.getFundType())) {
                    continue;
                }
            }
            String directionName = project == null ? null
                    : directionNames.get(project.getTechDirectionId());
            rows.add(new SnapshotRow(snapshot, project, groupId, groupNames.get(groupId), directionName));
        }
        return new TrendData(range, rows);
    }

    /**
     * 同一项目在同一时间粒度内只保留最新一次运行（runId 最大），避免重复计数。
     */
    public List<SnapshotRow> dedup(List<SnapshotRow> rows, Granularity granularity) {
        Map<String, SnapshotRow> latest = new LinkedHashMap<>();
        for (SnapshotRow row : rows) {
            String period = TrendSupport.periodOf(row.getSnapshot().getAnalysisDate(), granularity);
            String key = row.projectId() + "|" + period;
            SnapshotRow current = latest.get(key);
            if (current == null || runIdOf(row) > runIdOf(current)) {
                latest.put(key, row);
            }
        }
        List<SnapshotRow> result = new ArrayList<>(latest.values());
        result.sort(Comparator.comparing((SnapshotRow row) -> row.getSnapshot().getAnalysisDate())
                .thenComparing(SnapshotRow::projectId));
        return result;
    }

    public DateRange rangeOf(TrendQuery query) {
        return TrendSupport.resolveRange(query.getStartMonth(), query.getEndMonth());
    }

    public YearMonth monthOf(String period, Granularity granularity) {
        return YearMonth.from(TrendSupport.periodStart(period, granularity));
    }

    private long runIdOf(SnapshotRow row) {
        Long runId = row.getSnapshot().getRunId();
        return runId == null ? -1L : runId;
    }

    private Map<Long, String> loadGroupNames() {
        return groupMapper.selectList(Wrappers.<Group>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(Group::getId, Group::getName, (left, right) -> left, HashMap::new));
    }

    private Map<Long, String> loadDirectionNames() {
        return techDirectionMapper.selectList(Wrappers.<TechDirection>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(TechDirection::getId, TechDirection::getName,
                        (left, right) -> left, HashMap::new));
    }
}

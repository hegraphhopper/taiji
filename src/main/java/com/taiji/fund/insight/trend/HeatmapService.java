package com.taiji.fund.insight.trend;

import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.insight.trend.dto.HeatmapCellVO;
import com.taiji.fund.insight.trend.dto.HeatmapResponseVO;
import com.taiji.fund.insight.trend.dto.HeatmapRowVO;
import com.taiji.fund.insight.trend.dto.TrendQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 热力图：项目组×月份、技术方向×项目组；值为平均最高分或高风险项目数。
 */
@Service
@RequiredArgsConstructor
public class HeatmapService {

    public static final String GROUP_MONTH = "GROUP_MONTH";
    public static final String DIRECTION_GROUP = "DIRECTION_GROUP";

    private final TrendDataLoader dataLoader;

    @Transactional(readOnly = true)
    public HeatmapResponseVO heatmap(TrendQuery query, String dimension, String metric) {
        String normalized = dimension == null ? "" : dimension.trim().toUpperCase();
        if (!GROUP_MONTH.equals(normalized) && !DIRECTION_GROUP.equals(normalized)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "dimension 仅支持 GROUP_MONTH/DIRECTION_GROUP");
        }
        boolean highRiskMetric = "highRisk".equalsIgnoreCase(metric == null ? "" : metric.trim());
        String metricLabel = highRiskMetric ? "highRisk" : "avg";

        TrendScope scope = TrendScope.resolve(query);
        TrendData data = dataLoader.load(query, scope);
        List<SnapshotRow> rows = dataLoader.dedup(data.getRows(), Granularity.MONTH);

        List<String> columns;
        List<HeatmapRowVO> rowDefs = new ArrayList<>();
        List<HeatmapCellVO> cells = new ArrayList<>();
        if (GROUP_MONTH.equals(normalized)) {
            columns = periods(rows);
            Map<String, String> groups = new TreeMap<>();
            for (SnapshotRow row : rows) {
                String key = row.getGroupId() == null ? "none" : row.getGroupId().toString();
                groups.put(row.getGroupName() == null ? "未归属项目组" : row.getGroupName(), key);
            }
            Map<String, List<SnapshotRow>> buckets = new LinkedHashMap<>();
            for (SnapshotRow row : rows) {
                String groupKey = row.getGroupId() == null ? "none" : row.getGroupId().toString();
                String period = TrendSupport.periodOf(row.getSnapshot().getAnalysisDate(), Granularity.MONTH);
                buckets.computeIfAbsent(groupKey + "|" + period, ignored -> new ArrayList<>()).add(row);
            }
            for (String label : groups.keySet()) {
                String groupKey = groups.get(label);
                rowDefs.add(HeatmapRowVO.builder().key(groupKey).label(label).build());
                for (String period : columns) {
                    cells.add(cell(groupKey, period, buckets.get(groupKey + "|" + period), highRiskMetric));
                }
            }
        } else {
            List<String> groupLabels = groupLabels(rows);
            columns = groupLabels;
            Map<String, String> directions = new TreeMap<>();
            for (SnapshotRow row : rows) {
                String name = row.getTechDirectionName() == null ? "未选方向" : row.getTechDirectionName();
                directions.put(name, name);
            }
            Map<String, List<SnapshotRow>> buckets = new LinkedHashMap<>();
            for (SnapshotRow row : rows) {
                String direction = row.getTechDirectionName() == null ? "未选方向" : row.getTechDirectionName();
                String group = row.getGroupName() == null ? "未归属项目组" : row.getGroupName();
                buckets.computeIfAbsent(direction + "|" + group, ignored -> new ArrayList<>()).add(row);
            }
            for (String direction : directions.keySet()) {
                rowDefs.add(HeatmapRowVO.builder().key(direction).label(direction).build());
                for (String group : groupLabels) {
                    cells.add(cell(direction, group, buckets.get(direction + "|" + group), highRiskMetric));
                }
            }
        }

        return HeatmapResponseVO.builder()
                .hasData(!rows.isEmpty())
                .dimension(normalized)
                .metric(metricLabel)
                .columns(columns)
                .rows(rowDefs)
                .cells(cells)
                .note("无数据单元格表示该分组在该维度下没有项目快照。")
                .build();
    }

    private HeatmapCellVO cell(String rowKey, String column, List<SnapshotRow> rows, boolean highRiskMetric) {
        if (rows == null || rows.isEmpty()) {
            return HeatmapCellVO.builder()
                    .rowKey(rowKey)
                    .column(column)
                    .value(null)
                    .sampleCount(0)
                    .build();
        }
        if (highRiskMetric) {
            int high = 0;
            for (SnapshotRow row : rows) {
                if (row.highPair() > 0) {
                    high++;
                }
            }
            return HeatmapCellVO.builder()
                    .rowKey(rowKey)
                    .column(column)
                    .value(BigDecimal.valueOf(high))
                    .sampleCount(rows.size())
                    .build();
        }
        double sum = 0D;
        for (SnapshotRow row : rows) {
            sum += row.getSnapshot().getMaxScore() == null ? 0D
                    : row.getSnapshot().getMaxScore().doubleValue();
        }
        BigDecimal avg = BigDecimal.valueOf(sum / rows.size()).setScale(2, RoundingMode.HALF_UP);
        return HeatmapCellVO.builder()
                .rowKey(rowKey)
                .column(column)
                .value(avg)
                .sampleCount(rows.size())
                .build();
    }

    private List<String> periods(List<SnapshotRow> rows) {
        Set<String> periods = new LinkedHashSet<>();
        for (SnapshotRow row : rows) {
            periods.add(TrendSupport.periodOf(row.getSnapshot().getAnalysisDate(), Granularity.MONTH));
        }
        List<String> sorted = new ArrayList<>(periods);
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }

    private List<String> groupLabels(List<SnapshotRow> rows) {
        Set<String> labels = new LinkedHashSet<>();
        for (SnapshotRow row : rows) {
            labels.add(row.getGroupName() == null ? "未归属项目组" : row.getGroupName());
        }
        List<String> sorted = new ArrayList<>(labels);
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }
}

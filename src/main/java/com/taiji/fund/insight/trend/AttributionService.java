package com.taiji.fund.insight.trend;

import com.taiji.fund.insight.trend.dto.AttributionCountsVO;
import com.taiji.fund.insight.trend.dto.AttributionResponseVO;
import com.taiji.fund.insight.trend.dto.GroupAttributionVO;
import com.taiji.fund.insight.trend.dto.TrendQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 归因汇总：四类计数与占比、按项目组矩阵（项目视角）。
 */
@Service
@RequiredArgsConstructor
public class AttributionService {

    private final TrendDataLoader dataLoader;

    @Transactional(readOnly = true)
    public AttributionResponseVO attribution(TrendQuery query) {
        TrendScope scope = TrendScope.resolve(query);
        TrendData data = dataLoader.load(query, scope);
        List<SnapshotRow> rows = dataLoader.dedup(data.getRows(), Granularity.MONTH);

        int selfGroup = 0;
        int peerEarlier = 0;
        int ourEarlier = 0;
        int simultaneous = 0;
        Map<String, int[]> groupCounts = new LinkedHashMap<>();
        Map<String, String> groupNames = new LinkedHashMap<>();
        for (SnapshotRow row : rows) {
            selfGroup += row.selfGroup();
            peerEarlier += row.peerEarlier();
            ourEarlier += row.ourEarlier();
            simultaneous += row.simultaneous();
            String key = row.getGroupId() == null ? "none" : row.getGroupId().toString();
            String name = row.getGroupName() == null ? "未归属项目组" : row.getGroupName();
            groupNames.put(key, name);
            int[] counts = groupCounts.computeIfAbsent(key, ignored -> new int[4]);
            counts[0] += row.selfGroup();
            counts[1] += row.peerEarlier();
            counts[2] += row.ourEarlier();
            counts[3] += row.simultaneous();
        }

        AttributionCountsVO totalCounts = AttributionCountsVO.builder()
                .selfGroup(selfGroup)
                .peerEarlier(peerEarlier)
                .ourEarlier(ourEarlier)
                .simultaneous(simultaneous)
                .build();

        List<GroupAttributionVO> groups = new ArrayList<>(groupCounts.size());
        for (Map.Entry<String, int[]> entry : groupCounts.entrySet()) {
            int[] counts = entry.getValue();
            AttributionCountsVO groupCount = AttributionCountsVO.builder()
                    .selfGroup(counts[0])
                    .peerEarlier(counts[1])
                    .ourEarlier(counts[2])
                    .simultaneous(counts[3])
                    .build();
            groups.add(GroupAttributionVO.builder()
                    .groupId("none".equals(entry.getKey()) ? null : Long.valueOf(entry.getKey()))
                    .groupName(groupNames.get(entry.getKey()))
                    .counts(groupCount)
                    .total(groupCount.total())
                    .build());
        }
        groups.sort((left, right) -> Integer.compare(right.getTotal(), left.getTotal()));

        return AttributionResponseVO.builder()
                .hasData(!rows.isEmpty())
                .total(totalCounts.total())
                .counts(totalCounts)
                .ratios(totalCounts.ratios())
                .groups(groups)
                .note(TrendSupport.DEFAULT_NOTE)
                .build();
    }
}

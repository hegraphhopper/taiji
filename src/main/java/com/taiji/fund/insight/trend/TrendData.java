package com.taiji.fund.insight.trend;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 载入结果：解析后的月份范围与过滤后的快照行。
 */
@Getter
@AllArgsConstructor
public class TrendData {

    private final DateRange range;

    private final List<SnapshotRow> rows;
}

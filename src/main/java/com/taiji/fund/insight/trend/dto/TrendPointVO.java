package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单个时间点的趋势指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO {

    private String period;

    private int analyzedProjectCount;

    private BigDecimal avgMaxScore;

    private BigDecimal avgTopScore;

    private int highRiskCount;

    private int pairCount;

    private int uniquePairCount;

    private int highPairCount;

    private AttributionCountsVO attribution;

    private AttributionRatiosVO ratios;

    private TrendMomVO mom;
}

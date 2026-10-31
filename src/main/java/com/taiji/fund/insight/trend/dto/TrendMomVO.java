package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 环比：与上一个时间点比较；首点为 null。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendMomVO {

    private BigDecimal avgMaxScoreDelta;

    private BigDecimal avgMaxScoreRatio;
}

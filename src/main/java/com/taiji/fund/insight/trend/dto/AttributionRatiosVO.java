package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 四类归因占比（0–1，保留 4 位）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionRatiosVO {

    private BigDecimal selfGroup;

    private BigDecimal peerEarlier;

    private BigDecimal ourEarlier;

    private BigDecimal simultaneous;
}

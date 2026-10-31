package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 四类归因计数与占比（项目视角）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionCountsVO {

    private int selfGroup;

    private int peerEarlier;

    private int ourEarlier;

    private int simultaneous;

    public int total() {
        return selfGroup + peerEarlier + ourEarlier + simultaneous;
    }

    public AttributionRatiosVO ratios() {
        int total = total();
        if (total == 0) {
            return AttributionRatiosVO.builder()
                    .selfGroup(BigDecimal.ZERO)
                    .peerEarlier(BigDecimal.ZERO)
                    .ourEarlier(BigDecimal.ZERO)
                    .simultaneous(BigDecimal.ZERO)
                    .build();
        }
        return AttributionRatiosVO.builder()
                .selfGroup(ratio(selfGroup, total))
                .peerEarlier(ratio(peerEarlier, total))
                .ourEarlier(ratio(ourEarlier, total))
                .simultaneous(ratio(simultaneous, total))
                .build();
    }

    private BigDecimal ratio(int value, int total) {
        return BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(total), 4, BigDecimal.ROUND_HALF_UP);
    }
}

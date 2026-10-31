package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 归因汇总响应：区间内四类计数、占比与按组矩阵。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionResponseVO {

    private boolean hasData;

    private int total;

    private AttributionCountsVO counts;

    private AttributionRatiosVO ratios;

    private List<GroupAttributionVO> groups;

    private String note;
}

package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热力图行标识。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapRowVO {

    private String key;

    private String label;
}

package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 热力图单元格：无数据时 value 为 null。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapCellVO {

    private String rowKey;

    private String column;

    private BigDecimal value;

    private int sampleCount;
}

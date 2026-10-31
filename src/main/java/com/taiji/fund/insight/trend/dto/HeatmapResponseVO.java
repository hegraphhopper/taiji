package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 热力图响应：行列定义 + 单元格。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponseVO {

    private boolean hasData;

    private String dimension;

    private String metric;

    private List<String> columns;

    private List<HeatmapRowVO> rows;

    private List<HeatmapCellVO> cells;

    private String note;
}

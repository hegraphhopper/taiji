package com.taiji.fund.analysis.result.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 五维得分（0-100，保留 2 位小数）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionScoresVO {

    private double goal;

    private double content;

    private double tech;

    private double milestone;

    private double tag;
}

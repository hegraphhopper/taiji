package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 趋势点下钻的项目行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillProjectVO {

    private Long projectId;

    private String code;

    private String name;

    private Long groupId;

    private String groupName;

    private String techDirectionName;

    private String fundType;

    private BigDecimal maxScore;

    private String riskLevel;

    private int attributionTotal;

    private LocalDate analysisDate;
}

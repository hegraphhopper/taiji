package com.taiji.fund.insight.investment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 投入下钻/风险列表中的项目行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentProjectVO {

    private Long projectId;

    private String code;

    private String name;

    private Long groupId;

    private String groupName;

    private String techDirectionName;

    private String fundType;

    private String status;

    private BigDecimal budget;

    private BigDecimal maxScore;

    private String riskLevel;

    private BigDecimal exposure;

    private Integer year;
}

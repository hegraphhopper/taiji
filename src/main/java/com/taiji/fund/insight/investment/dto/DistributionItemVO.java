package com.taiji.fund.insight.investment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单维度分布项（年度/基金类型/项目组/技术方向通用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionItemVO {

    private String key;

    private String label;

    private int projectCount;

    private BigDecimal budget;

    private BigDecimal budgetShare;

    private BigDecimal avgMaxScore;

    private int highRiskCount;

    private BigDecimal exposure;
}

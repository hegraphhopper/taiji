package com.taiji.fund.insight.investment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 投入概览 KPI。金额单位为元，比例保留 4 位。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentOverviewVO {

    private boolean hasData;

    private BigDecimal totalBudget;

    private int projectCount;

    private int inProgressCount;

    private int completedCount;

    private BigDecimal avgBudget;

    private int analyzedCount;

    private int unanalyzedCount;

    private BigDecimal avgMaxScore;

    private int highRiskCount;

    private BigDecimal exposure;

    private BigDecimal highExposure;

    private BigDecimal yoyBudgetRatio;

    private BigDecimal yoyCountRatio;

    private Map<String, Integer> thresholds;

    private String note;
}

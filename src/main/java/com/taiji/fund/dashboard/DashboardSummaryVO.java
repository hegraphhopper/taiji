package com.taiji.fund.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 工作台汇总指标：按当前用户可见项目统计。
 */
@Data
@Builder
public class DashboardSummaryVO {

    private int projectCount;

    private int analyzedCount;

    private BigDecimal avgMaxScore;

    private int highRiskCount;

    private Map<String, Integer> thresholds;
}

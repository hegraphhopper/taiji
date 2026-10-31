package com.taiji.fund.insight.trend.dto;

import lombok.Data;

/**
 * 趋势/归因/热力图查询参数。startMonth、endMonth 形如 2026-05；缺省为最近 12 个月。
 */
@Data
public class TrendQuery {

    private String startMonth;

    private String endMonth;

    private String granularity;

    private Long groupId;

    private Long techDirectionId;

    private String fundType;
}

package com.taiji.fund.insight.trend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 趋势点下钻：在筛选条件基础上指定 period 与可选等级。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrendDrillQuery extends TrendQuery {

    private String period;

    private String level;
}

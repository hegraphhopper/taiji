package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 趋势看板响应：时间点、摘要与口径参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponseVO {

    private boolean hasData;

    private String granularity;

    private String startMonth;

    private String endMonth;

    private List<TrendPointVO> points;

    private List<String> summary;

    private Map<String, Integer> thresholds;

    private String note;
}

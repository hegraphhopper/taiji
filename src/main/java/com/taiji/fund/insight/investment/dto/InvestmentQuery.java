package com.taiji.fund.insight.investment.dto;

import lombok.Data;

/**
 * 投入态势筛选：年度范围、基金类型、项目组、技术方向、是否含结项。
 */
@Data
public class InvestmentQuery {

    private Integer startYear;

    private Integer endYear;

    private String fundType;

    private Long groupId;

    private Long techDirectionId;

    private Boolean includeCompleted;
}

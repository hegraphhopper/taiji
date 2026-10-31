package com.taiji.fund.analysis.result.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 运行历史查询条件：类型、状态、时间区间与分页。
 */
@Data
public class AnalysisRunQuery {

    private String triggerType;

    private String status;

    private LocalDate from;

    private LocalDate to;

    private long page = 1;

    private long size = 10;
}

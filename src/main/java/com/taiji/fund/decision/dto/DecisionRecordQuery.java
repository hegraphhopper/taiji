package com.taiji.fund.decision.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 决策记录查询：按项目、组、等级、建议与时间筛选。
 */
@Data
public class DecisionRecordQuery {

    private Long projectId;

    private Long groupId;

    private String level;

    private String recommendation;

    private LocalDate from;

    private LocalDate to;

    private Long page = 1L;

    private Long size = 10L;
}

package com.taiji.fund.decision.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 保存决策记录：保存预检结果快照，不重新计算。
 */
@Data
public class DecisionSaveRequest {

    private Long projectId;

    private Long runId;

    private BigDecimal riskScore;

    private String level;

    private String recommendation;

    private String reason;

    private String similarProjects;

    private BigDecimal savingsEstimate;

    private String inputHash;
}

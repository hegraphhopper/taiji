package com.taiji.fund.decision.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 决策规则保存项：管理员整体替换规则表。
 */
@Data
public class DecisionRuleItem {

    private Integer priority;

    private BigDecimal minScore;

    private BigDecimal maxScore;

    private String relation;

    private Integer sameGroup;

    private String action;

    private String description;

    private Integer enabled;
}

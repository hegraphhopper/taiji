package com.taiji.fund.decision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 辅助决策预检请求：PROJECT 复用项目内容，TEXT 使用临时文本。
 */
@Data
public class DecisionPrecheckRequest {

    private String mode;

    private Long projectId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String goalText;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contentText;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String techText;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String milestoneText;

    private Long techDirectionId;

    private BigDecimal budget;
}

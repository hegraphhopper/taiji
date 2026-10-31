package com.taiji.fund.decision.dto;

import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 辅助决策预检结果：风险分、Top-5 相似项目、建议动作、金额与专家。
 */
@Data
@Builder
public class DecisionPrecheckVO {

    private String mode;

    private Long runId;

    private Long projectId;

    private String projectCode;

    private String projectName;

    private String groupName;

    private String techDirectionName;

    private BigDecimal budget;

    private BigDecimal riskScore;

    private String level;

    private List<PeerMatchVO> topPeers;

    private String recommendation;

    private Long ruleId;

    private String ruleDescription;

    private String reason;

    private BigDecimal savingsEstimate;

    private List<SuggestedExpertVO> suggestedExperts;

    private Boolean llmUsed;

    private String inputHash;
}

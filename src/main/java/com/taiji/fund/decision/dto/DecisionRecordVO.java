package com.taiji.fund.decision.dto;

import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 决策记录：列表与详情共用，详情返回 Top 相似项目快照。
 */
@Data
@Builder
public class DecisionRecordVO {

    private Long id;

    private Long projectId;

    private String projectCode;

    private String projectName;

    private String groupName;

    private Long runId;

    private BigDecimal riskScore;

    private String level;

    private String recommendation;

    private String reason;

    private List<PeerMatchVO> similarProjects;

    private BigDecimal savingsEstimate;

    private String inputHash;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime createdAt;
}

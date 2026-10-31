package com.taiji.fund.analysis.result.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目摘要，用于项目报告、项目对明细与预检结果中的双方信息。
 */
@Data
@Builder
public class AnalysisProjectBriefVO {

    private Long id;

    private String code;

    private String name;

    private Long groupId;

    private String groupName;

    private Long ownerId;

    private String ownerName;

    private Long techDirectionId;

    private String techDirectionName;

    private String status;

    private BigDecimal budget;

    private LocalDateTime submitTime;
}

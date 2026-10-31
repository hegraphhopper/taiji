package com.taiji.fund.analysis.result.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DuplicateSnapshotVO {

    private Long id;

    private Long runId;

    private Long projectId;

    private Long groupId;

    private LocalDate analysisDate;

    private BigDecimal maxScore;

    private BigDecimal avgTopScore;

    private Integer highPairCount;

    private Integer crossGroupCount;

    private Integer selfGroupCount;

    private Integer ourEarlierCount;

    private Integer peerEarlierCount;

    private Integer simultaneousCount;

    private BigDecimal riskExposure;

    private LocalDateTime createdAt;
}

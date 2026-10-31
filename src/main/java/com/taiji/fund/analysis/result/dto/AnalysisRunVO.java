package com.taiji.fund.analysis.result.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnalysisRunVO {

    private Long id;

    private String triggerType;

    private Long projectId;

    private Integer configVersion;

    private LocalDateTime analysisDate;

    private String status;

    private Integer projectCount;

    private Integer pairCount;

    private Boolean llmUsed;

    private Long durationMs;

    private String message;

    private Long operatorId;

    private LocalDateTime createdAt;
}

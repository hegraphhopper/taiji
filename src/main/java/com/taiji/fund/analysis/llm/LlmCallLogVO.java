package com.taiji.fund.analysis.llm;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LlmCallLogVO {

    private Long id;

    private Long runId;

    private Long projectId;

    private Long peerProjectId;

    private String model;

    private String status;

    private Long latencyMs;

    private Integer promptTokens;

    private Integer completionTokens;

    private String error;

    private LocalDateTime createdAt;
}

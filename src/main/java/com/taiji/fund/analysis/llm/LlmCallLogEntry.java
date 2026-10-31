package com.taiji.fund.analysis.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调用日志元数据：不含 prompt 正文、API Key 与项目正文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmCallLogEntry {

    private Long runId;

    private Long projectId;

    private Long peerProjectId;

    private String model;

    private LlmStatus status;

    private long latencyMs;

    private Integer promptTokens;

    private Integer completionTokens;

    private String error;
}

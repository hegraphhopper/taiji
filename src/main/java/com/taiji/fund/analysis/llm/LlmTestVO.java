package com.taiji.fund.analysis.llm;

import lombok.Builder;
import lombok.Data;

/**
 * 大模型连接自检结果。不包含 API Key。
 */
@Data
@Builder
public class LlmTestVO {

    private boolean success;

    private String status;

    private long latencyMs;

    private String model;

    private String message;
}

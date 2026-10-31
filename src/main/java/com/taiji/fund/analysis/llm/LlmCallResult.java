package com.taiji.fund.analysis.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次大模型调用结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmCallResult {

    private LlmStatus status;

    private LlmResponse response;

    private long latencyMs;

    private Integer promptTokens;

    private Integer completionTokens;

    private String error;

    private boolean cached;

    public boolean isSuccess() {
        return status == LlmStatus.SUCCESS;
    }

    public Double getSemanticScore() {
        return response == null ? null : response.getSemanticScore();
    }

    public static LlmCallResult disabled(String message) {
        return LlmCallResult.builder()
                .status(LlmStatus.DISABLED)
                .error(message)
                .build();
    }

    public static LlmCallResult cached(LlmResponse response) {
        return LlmCallResult.builder()
                .status(LlmStatus.SUCCESS)
                .response(response)
                .cached(true)
                .build();
    }

    public static LlmCallResult success(LlmResponse response, long latencyMs,
                                        Integer promptTokens, Integer completionTokens) {
        return LlmCallResult.builder()
                .status(LlmStatus.SUCCESS)
                .response(response)
                .latencyMs(latencyMs)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
    }

    public static LlmCallResult failure(LlmStatus status, long latencyMs, String error) {
        return LlmCallResult.builder()
                .status(status)
                .latencyMs(latencyMs)
                .error(error)
                .build();
    }
}

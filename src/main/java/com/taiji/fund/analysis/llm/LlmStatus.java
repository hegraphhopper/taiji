package com.taiji.fund.analysis.llm;

/**
 * 大模型调用状态。
 */
public enum LlmStatus {

    SUCCESS,
    TIMEOUT,
    HTTP_ERROR,
    INVALID,
    DISABLED
}

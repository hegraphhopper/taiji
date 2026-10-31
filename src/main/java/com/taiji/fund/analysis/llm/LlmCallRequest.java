package com.taiji.fund.analysis.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次大模型调用请求。textHash 用于缓存 key，prompt 只用于 HTTP 请求，不写日志。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmCallRequest {

    private Long runId;

    private Long projectId;

    private Long peerId;

    private String textHash;

    private int configVersion;

    private String userPrompt;
}

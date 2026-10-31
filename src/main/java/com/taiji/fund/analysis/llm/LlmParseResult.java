package com.taiji.fund.analysis.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型响应解析结果。非法响应不抛异常，由调用方降级为本地分。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmParseResult {

    private boolean valid;

    private LlmResponse response;

    private String error;

    public static LlmParseResult valid(LlmResponse response) {
        return new LlmParseResult(true, response, null);
    }

    public static LlmParseResult invalid(String error) {
        return new LlmParseResult(false, null, error);
    }
}

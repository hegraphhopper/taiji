package com.taiji.fund.analysis.llm;

/**
 * HTTP 传输抽象：便于单元测试用桩替换，不依赖外网。
 *
 * <p>apiKey 只用于 Authorization 头，禁止写入日志与错误信息。</p>
 */
public interface LlmHttpTransport {

    LlmHttpResponse post(String url, String jsonBody, String apiKey, int timeoutMs);

    default LlmHttpResponse post(String url, String jsonBody, int timeoutMs) {
        return post(url, jsonBody, null, timeoutMs);
    }
}

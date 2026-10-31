package com.taiji.fund.analysis.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容 chat/completions 客户端。
 *
 * <p>未启用/未配置时返回 DISABLED，不抛异常；超时、HTTP 非 2xx、非法 JSON 均降级；
 * 单次运行连续失败 3 次后由 {@link LlmCircuitBreaker} 阻断后续调用。
 * 日志只记录元数据，绝不写入 prompt、API Key 或项目正文。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private static final String SYSTEM_PROMPT = "你是科研项目查重专家，只输出 JSON。";
    private static final int MAX_ERROR_LENGTH = 500;

    private final LlmHttpTransport transport;
    private final LlmResponseParser responseParser;
    private final LlmCache cache;
    private final LlmCallRecorder recorder;
    private final ObjectMapper objectMapper;

    public LlmCallResult call(LlmCallRequest request, LlmOptions options, LlmCircuitBreaker breaker) {
        if (request == null || options == null || !options.usable()) {
            return LlmCallResult.disabled("大模型未启用或未配置");
        }
        LlmCircuitBreaker effectiveBreaker = breaker == null ? new LlmCircuitBreaker() : breaker;
        if (effectiveBreaker.isOpen()) {
            return LlmCallResult.disabled("连续失败达到阈值，本次运行跳过大模型");
        }

        String cacheKey = cache.key(request.getProjectId(), request.getPeerId(),
                request.getTextHash(), request.getConfigVersion());
        LlmResponse cached = cache.get(cacheKey);
        if (cached != null) {
            return LlmCallResult.cached(cached);
        }

        long start = System.currentTimeMillis();
        String requestBody;
        try {
            requestBody = buildRequestBody(options, request.getUserPrompt());
        } catch (Exception exception) {
            long latency = System.currentTimeMillis() - start;
            effectiveBreaker.recordFailure();
            record(request, options, LlmStatus.INVALID, latency, null, null, "请求构造失败");
            return LlmCallResult.failure(LlmStatus.INVALID, latency, "请求构造失败");
        }

        LlmHttpResponse httpResponse;
        try {
            httpResponse = transport.post(options.endpoint(), requestBody,
                    options.getApiKey(), Math.max(500, options.getTimeoutMs()));
        } catch (Exception exception) {
            long latency = System.currentTimeMillis() - start;
            effectiveBreaker.recordFailure();
            String error = "CONNECTION_ERROR";
            record(request, options, LlmStatus.HTTP_ERROR, latency, null, null, error);
            return LlmCallResult.failure(LlmStatus.HTTP_ERROR, latency, error);
        }
        long latency = System.currentTimeMillis() - start;
        if (httpResponse == null || httpResponse.getStatusCode() == 0) {
            LlmStatus status = httpResponse != null && httpResponse.isTimeout()
                    ? LlmStatus.TIMEOUT : LlmStatus.HTTP_ERROR;
            String error = httpResponse == null ? "CONNECTION_ERROR"
                    : (httpResponse.getError() == null ? "CONNECTION_ERROR" : httpResponse.getError());
            effectiveBreaker.recordFailure();
            record(request, options, status, latency, null, null, error);
            return LlmCallResult.failure(status, latency, error);
        }
        if (httpResponse.getStatusCode() < 200 || httpResponse.getStatusCode() >= 300) {
            effectiveBreaker.recordFailure();
            String error = "HTTP " + httpResponse.getStatusCode();
            record(request, options, LlmStatus.HTTP_ERROR, latency, null, null, error);
            return LlmCallResult.failure(LlmStatus.HTTP_ERROR, latency, error);
        }

        String content = extractContent(httpResponse.getBody());
        LlmParseResult parseResult = responseParser.parse(content);
        if (!parseResult.isValid()) {
            effectiveBreaker.recordFailure();
            record(request, options, LlmStatus.INVALID, latency, null, null, parseResult.getError());
            return LlmCallResult.failure(LlmStatus.INVALID, latency, parseResult.getError());
        }

        Integer promptTokens = extractTokens(httpResponse.getBody(), "prompt_tokens");
        Integer completionTokens = extractTokens(httpResponse.getBody(), "completion_tokens");
        cache.put(cacheKey, parseResult.getResponse());
        effectiveBreaker.recordSuccess();
        record(request, options, LlmStatus.SUCCESS, latency, promptTokens, completionTokens, null);
        return LlmCallResult.success(parseResult.getResponse(), latency, promptTokens, completionTokens);
    }

    private String buildRequestBody(LlmOptions options, String userPrompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", options.getModel() == null ? "gpt-4o-mini" : options.getModel());
        root.put("temperature", 0);
        ObjectNode format = root.putObject("response_format");
        format.put("type", "json_object");
        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt == null ? "" : userPrompt);
        return objectMapper.writeValueAsString(root);
    }

    private String extractContent(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isTextual() ? content.asText() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private Integer extractTokens(String body, String field) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body).path("usage").path(field);
            return node.isNumber() ? node.asInt() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private void record(LlmCallRequest request, LlmOptions options, LlmStatus status,
                        long latency, Integer promptTokens, Integer completionTokens, String error) {
        try {
            recorder.record(LlmCallLogEntry.builder()
                    .runId(request.getRunId())
                    .projectId(request.getProjectId())
                    .peerProjectId(request.getPeerId())
                    .model(options.getModel())
                    .status(status)
                    .latencyMs(latency)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .error(truncate(error))
                    .build());
        } catch (Exception exception) {
            log.warn("大模型调用日志记录失败: {}", exception.getMessage());
        }
    }

    private String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }
}

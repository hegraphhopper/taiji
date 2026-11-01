package com.taiji.fund.analysis.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taiji.fund.analysis.score.ScoreFusion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LlmFallbackTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private StubTransport transport;
    private List<LlmCallLogEntry> logs;
    private LlmCache cache;
    private LlmClient client;

    @BeforeEach
    void setUp() {
        transport = new StubTransport();
        logs = new ArrayList<>();
        cache = new LlmCache();
        client = new LlmClient(transport, new LlmResponseParser(mapper), cache, logs::add, mapper);
    }

    @Test
    @DisplayName("UT-18 大模型关闭时返回 DISABLED 且不发起调用、不写日志")
    void ut18_disabled() {
        LlmCallResult result = client.call(request("h1"), options(false), new LlmCircuitBreaker());

        assertThat(result.getStatus()).isEqualTo(LlmStatus.DISABLED);
        assertThat(transport.calls).isZero();
        assertThat(logs).isEmpty();
    }

    @Test
    @DisplayName("UT-20 合法响应融合本地分与大模型分")
    void ut20_successAndFusion() throws Exception {
        transport.responses.add(success(validContent(90D)));
        LlmCallResult result = client.call(request("h2"), options(true), new LlmCircuitBreaker());

        assertThat(result.getStatus()).isEqualTo(LlmStatus.SUCCESS);
        assertThat(result.getSemanticScore()).isEqualTo(90D);
        assertThat(result.isCached()).isFalse();
        assertThat(result.getPromptTokens()).isEqualTo(12);
        assertThat(result.getCompletionTokens()).isEqualTo(34);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus()).isEqualTo(LlmStatus.SUCCESS);
        assertThat(logs.get(0).toString()).doesNotContain("PROMPT-CONTENT").doesNotContain("sk-secret");

        assertThat(new ScoreFusion().fuse(70D, result.getSemanticScore(), 70, 30))
                .isCloseTo(76D, within(0.01));
    }

    @Test
    @DisplayName("UT-33 HTTP 500 重试耗尽后降级，只记一条 HTTP_ERROR")
    void ut33_httpErrorRetryExhausted() {
        transport.responses.add(LlmHttpResponse.builder().statusCode(500).body("error").build());
        transport.responses.add(LlmHttpResponse.builder().statusCode(500).body("error").build());
        LlmCircuitBreaker breaker = new LlmCircuitBreaker();
        LlmCallResult result = client.call(request("h3"), options(true), breaker);

        assertThat(result.getStatus()).isEqualTo(LlmStatus.HTTP_ERROR);
        assertThat(transport.calls).isEqualTo(2);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus()).isEqualTo(LlmStatus.HTTP_ERROR);
        assertThat(logs.get(0).getError()).isEqualTo("HTTP 500");
        assertThat(breaker.failures()).isEqualTo(1);
    }

    @Test
    @DisplayName("UT-32 超时后重试一次成功")
    void ut32_timeoutThenSuccess() throws Exception {
        transport.responses.add(LlmHttpResponse.builder().statusCode(0).timeout(true).error("TIMEOUT").build());
        transport.responses.add(success(validContent(88D)));
        LlmCircuitBreaker breaker = new LlmCircuitBreaker();
        LlmCallResult result = client.call(request("h3-retry"), options(true), breaker);

        assertThat(result.getStatus()).isEqualTo(LlmStatus.SUCCESS);
        assertThat(result.getSemanticScore()).isEqualTo(88D);
        assertThat(transport.calls).isEqualTo(2);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus()).isEqualTo(LlmStatus.SUCCESS);
        assertThat(breaker.failures()).isZero();
    }

    @Test
    @DisplayName("UT-34 HTTP 400 不重试")
    void ut34_clientErrorNoRetry() {
        transport.responses.add(LlmHttpResponse.builder().statusCode(400).body("bad request").build());
        LlmCallResult result = client.call(request("h3-400"), options(true), new LlmCircuitBreaker());

        assertThat(result.getStatus()).isEqualTo(LlmStatus.HTTP_ERROR);
        assertThat(transport.calls).isEqualTo(1);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getError()).isEqualTo("HTTP 400");
    }

    @Test
    @DisplayName("UT-19 超时重试后仍失败，记一条 TIMEOUT")
    void ut19_timeout() {
        transport.responses.add(LlmHttpResponse.builder().statusCode(0).timeout(true).error("TIMEOUT").build());
        transport.responses.add(LlmHttpResponse.builder().statusCode(0).timeout(true).error("TIMEOUT").build());
        LlmCallResult result = client.call(request("h4"), options(true), new LlmCircuitBreaker());

        assertThat(result.getStatus()).isEqualTo(LlmStatus.TIMEOUT);
        assertThat(transport.calls).isEqualTo(2);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus()).isEqualTo(LlmStatus.TIMEOUT);
    }

    @Test
    @DisplayName("UT-19 连接失败重试后归类为 HTTP_ERROR")
    void ut19_connectionError() {
        transport.responses.add(LlmHttpResponse.builder().statusCode(0).timeout(false)
                .error("CONNECTION_ERROR").build());
        transport.responses.add(LlmHttpResponse.builder().statusCode(0).timeout(false)
                .error("CONNECTION_ERROR").build());
        LlmCallResult result = client.call(request("h5"), options(true), new LlmCircuitBreaker());

        assertThat(result.getStatus()).isEqualTo(LlmStatus.HTTP_ERROR);
        assertThat(transport.calls).isEqualTo(2);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getError()).isEqualTo("CONNECTION_ERROR");
    }

    @Test
    @DisplayName("UT-19 非法 JSON 降级并记录 INVALID")
    void ut19_invalidJson() throws Exception {
        transport.responses.add(success("not-json"));
        LlmCallResult result = client.call(request("h6"), options(true), new LlmCircuitBreaker());

        assertThat(result.getStatus()).isEqualTo(LlmStatus.INVALID);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus()).isEqualTo(LlmStatus.INVALID);
        assertThat(logs.get(0).getError()).doesNotContain("PROMPT-CONTENT").doesNotContain("sk-secret");
    }

    @Test
    @DisplayName("UT-19 连续失败 3 次后熔断，第 4 次不再调用")
    void ut19_circuitBreaker() {
        for (int index = 0; index < 6; index++) {
            transport.responses.add(LlmHttpResponse.builder().statusCode(0).timeout(true)
                    .error("TIMEOUT").build());
        }
        LlmCircuitBreaker breaker = new LlmCircuitBreaker();
        LlmCallResult first = client.call(request("h7"), options(true), breaker);
        LlmCallResult second = client.call(request("h8"), options(true), breaker);
        LlmCallResult third = client.call(request("h9"), options(true), breaker);
        LlmCallResult fourth = client.call(request("h10"), options(true), breaker);

        assertThat(first.getStatus()).isEqualTo(LlmStatus.TIMEOUT);
        assertThat(second.getStatus()).isEqualTo(LlmStatus.TIMEOUT);
        assertThat(third.getStatus()).isEqualTo(LlmStatus.TIMEOUT);
        assertThat(fourth.getStatus()).isEqualTo(LlmStatus.DISABLED);
        assertThat(transport.calls).isEqualTo(6);
        assertThat(logs).hasSize(3);
        assertThat(breaker.isOpen()).isTrue();
        breaker.reset();
        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("UT-21 相同 key 命中缓存不重复调用与计费")
    void ut21_cacheHitSkipsTransport() throws Exception {
        transport.responses.add(success(validContent(85D)));
        LlmOptions options = options(true);
        LlmCallRequest request = request("h-cache");

        LlmCallResult first = client.call(request, options, new LlmCircuitBreaker());
        LlmCallResult second = client.call(request, options, new LlmCircuitBreaker());

        assertThat(first.isCached()).isFalse();
        assertThat(second.isCached()).isTrue();
        assertThat(second.getSemanticScore()).isEqualTo(85D);
        assertThat(transport.calls).isEqualTo(1);
        assertThat(logs).hasSize(1);
        assertThat(cache.size()).isEqualTo(1);
    }

    private LlmCallRequest request(String textHash) {
        return LlmCallRequest.builder()
                .runId(1L)
                .projectId(1L)
                .peerId(2L)
                .textHash(textHash)
                .configVersion(1)
                .userPrompt("PROMPT-CONTENT-SECRET")
                .build();
    }

    private LlmOptions options(boolean enabled) {
        return LlmOptions.builder()
                .enabled(enabled)
                .baseUrl("http://localhost:18080/llm")
                .apiKey("sk-secret")
                .model("test-model")
                .timeoutMs(1000)
                .build();
    }

    private LlmHttpResponse success(String content) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode message = root.putArray("choices").addObject().putObject("message");
        message.put("role", "assistant");
        message.put("content", content);
        ObjectNode usage = root.putObject("usage");
        usage.put("prompt_tokens", 12);
        usage.put("completion_tokens", 34);
        return LlmHttpResponse.builder()
                .statusCode(200)
                .body(mapper.writeValueAsString(root))
                .build();
    }

    private String validContent(double score) {
        return "{\"semanticScore\":" + score + ",\"overlaps\":[\"目标一致\"],"
                + "\"differences\":[\"场景不同\"],\"suggestion\":\"EXPLAIN\",\"reason\":\"存在重叠\"}";
    }

    private static class StubTransport implements LlmHttpTransport {

        private final Deque<LlmHttpResponse> responses = new ArrayDeque<>();
        private int calls;

        @Override
        public LlmHttpResponse post(String url, String jsonBody, String apiKey, int timeoutMs) {
            calls++;
            return responses.poll();
        }
    }
}

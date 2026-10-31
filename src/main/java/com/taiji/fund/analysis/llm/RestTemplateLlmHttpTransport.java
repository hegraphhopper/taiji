package com.taiji.fund.analysis.llm;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 基于 RestTemplate 的 OpenAI 兼容接口传输实现。每次调用按配置超时创建客户端，0 重试。
 */
@Component
public class RestTemplateLlmHttpTransport implements LlmHttpTransport {

    @Override
    public LlmHttpResponse post(String url, String jsonBody, String apiKey, int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.setBearerAuth(apiKey.trim());
        }
        HttpEntity<byte[]> entity = new HttpEntity<>(
                jsonBody == null ? new byte[0] : jsonBody.getBytes(StandardCharsets.UTF_8), headers);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
            String body = response.getBody() == null ? "" : new String(response.getBody(), StandardCharsets.UTF_8);
            return LlmHttpResponse.builder()
                    .statusCode(response.getStatusCodeValue())
                    .body(body)
                    .build();
        } catch (HttpStatusCodeException exception) {
            return LlmHttpResponse.builder()
                    .statusCode(exception.getRawStatusCode())
                    .body(new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8))
                    .error("HTTP " + exception.getRawStatusCode())
                    .build();
        } catch (ResourceAccessException exception) {
            boolean timeout = isTimeout(exception);
            return LlmHttpResponse.builder()
                    .statusCode(0)
                    .timeout(timeout)
                    .error(timeout ? "TIMEOUT" : "CONNECTION_ERROR")
                    .build();
        } catch (Exception exception) {
            return LlmHttpResponse.builder()
                    .statusCode(0)
                    .timeout(false)
                    .error(exception.getClass().getSimpleName())
                    .build();
        }
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

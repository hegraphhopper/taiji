package com.taiji.fund.analysis.llm;

import com.taiji.fund.analysis.config.AnalysisConfigService;
import com.taiji.fund.analysis.config.AnalysisConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 连接自检：使用当前环境配置发起一次最小请求，验证地址、模型与密钥可用。
 */
@Service
@RequiredArgsConstructor
public class LlmConnectionTestService {

    private static final String TEST_PROMPT = "这是一次连接测试。请只输出 JSON："
            + "{\"semanticScore\":50,\"overlaps\":[],\"differences\":[],"
            + "\"suggestion\":\"EXPLAIN\",\"reason\":\"连接测试\"}";

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final AnalysisConfigService configService;

    public LlmTestVO test() {
        int timeout = resolveTimeout();
        LlmOptions options = llmProperties.toOptions(true, timeout);
        String model = options.getModel();
        if (!options.usable()) {
            return LlmTestVO.builder()
                    .success(false)
                    .status(LlmStatus.DISABLED.name())
                    .latencyMs(0)
                    .model(model)
                    .message("大模型未启用或未配置 LLM_BASE_URL")
                    .build();
        }
        LlmCallResult result = llmClient.call(LlmCallRequest.builder()
                .runId(null)
                .projectId(null)
                .peerId(null)
                .textHash("connection-test")
                .configVersion(0)
                .userPrompt(TEST_PROMPT)
                .build(), options, new LlmCircuitBreaker());
        return LlmTestVO.builder()
                .success(result.isSuccess())
                .status(result.getStatus() == null ? "UNKNOWN" : result.getStatus().name())
                .latencyMs(result.getLatencyMs())
                .model(model)
                .message(result.isSuccess() ? "连接成功" : safeMessage(result.getError()))
                .build();
    }

    private int resolveTimeout() {
        try {
            AnalysisConfigVO config = configService.current();
            if (config != null && config.getLlmTimeoutMs() != null) {
                return config.getLlmTimeoutMs();
            }
        } catch (RuntimeException ignored) {
            // 配置读取失败不影响连接测试，使用默认超时
        }
        return 8000;
    }

    private String safeMessage(String error) {
        if (error == null || error.trim().isEmpty()) {
            return "连接失败";
        }
        return error.length() > 200 ? error.substring(0, 200) : error;
    }
}

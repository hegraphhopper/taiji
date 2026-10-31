package com.taiji.fund.analysis.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型连接配置。默认关闭；密钥仅从环境变量读取，禁止写入日志。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private boolean enabled = false;

    private String baseUrl = "";

    private String model = "gpt-4o-mini";

    private String apiKey = "";

    /**
     * 合并数据库配置开关与运行超时，供分析运行编排使用。
     */
    public LlmOptions toOptions(boolean configEnabled, int configTimeoutMs) {
        return LlmOptions.builder()
                .enabled(enabled && configEnabled)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .timeoutMs(configTimeoutMs > 0 ? configTimeoutMs : 8000)
                .build();
    }
}

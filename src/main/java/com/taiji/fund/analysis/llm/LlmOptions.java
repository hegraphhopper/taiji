package com.taiji.fund.analysis.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次调用参数。baseUrl 与密钥来自环境变量，不写入数据库与日志。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmOptions {

    private boolean enabled;

    private String baseUrl;

    private String apiKey;

    private String model;

    private int timeoutMs;

    public boolean usable() {
        return enabled && baseUrl != null && !baseUrl.trim().isEmpty();
    }

    public String endpoint() {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/chat/completions";
    }
}

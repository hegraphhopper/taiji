package com.taiji.fund.analysis.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 传输层结果：statusCode=0 表示连接失败/超时。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmHttpResponse {

    private int statusCode;

    private String body;

    private boolean timeout;

    private String error;
}

package com.taiji.fund.analysis.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 校验后的大模型返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {

    private double semanticScore;

    private List<String> overlaps;

    private List<String> differences;

    private String suggestion;

    private String reason;
}

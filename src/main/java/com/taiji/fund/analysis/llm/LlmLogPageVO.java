package com.taiji.fund.analysis.llm;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LlmLogPageVO {

    private List<LlmCallLogVO> records;

    private long total;

    private long page;

    private long size;

    private long successCount;

    private long failureCount;

    private long avgLatencyMs;

    private long promptTokens;

    private long completionTokens;
}

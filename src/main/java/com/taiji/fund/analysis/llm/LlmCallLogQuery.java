package com.taiji.fund.analysis.llm;

import lombok.Data;

@Data
public class LlmCallLogQuery {

    private Long page = 1L;

    private Long size = 20L;

    private Long runId;

    private String status;

    private String model;
}

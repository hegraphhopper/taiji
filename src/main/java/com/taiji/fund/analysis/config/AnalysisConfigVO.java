package com.taiji.fund.analysis.config;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AnalysisConfigVO {

    private Long id;

    private Integer version;

    private Map<String, Integer> fieldWeights;

    private Map<String, Integer> thresholds;

    private Integer topK;

    private Integer simultaneousDays;

    private Boolean llmEnabled;

    private Integer localWeight;

    private Integer llmWeight;

    private Integer llmTopN;

    private Integer llmTimeoutMs;

    private Boolean active;

    private Long createdBy;

    private LocalDateTime createdAt;
}

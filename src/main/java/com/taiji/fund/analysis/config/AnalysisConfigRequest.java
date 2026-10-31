package com.taiji.fund.analysis.config;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 分析配置保存请求。权重与阈值通过 JSON 对象提交，服务端负责校验与序列化。
 */
@Data
public class AnalysisConfigRequest {

    @NotNull(message = "字段权重不能为空")
    private Map<String, Integer> fieldWeights;

    @NotNull(message = "等级阈值不能为空")
    private Map<String, Integer> thresholds;

    private Integer topK;

    private Integer simultaneousDays;

    private Boolean llmEnabled;

    private Integer localWeight;

    private Integer llmWeight;

    private Integer llmTopN;

    private Integer llmTimeoutMs;
}

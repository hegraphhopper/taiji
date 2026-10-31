package com.taiji.fund.analysis.result.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 预检请求：可指定草稿项目，或直接提交临时文本（目标/内容/技术/节点纯文本）。
 *
 * <p>文本字段标记为 WRITE_ONLY，保证审计切面序列化入参时不会把申报正文写入审计详情。</p>
 */
@Data
public class AnalysisPrecheckRequest {

    private Long projectId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String goalText;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contentText;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String techText;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String milestoneText;

    private Long techDirectionId;
}

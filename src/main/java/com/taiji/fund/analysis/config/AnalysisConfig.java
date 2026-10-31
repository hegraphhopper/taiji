package com.taiji.fund.analysis.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_config")
public class AnalysisConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer version;

    /**
     * JSON 字符串，例如 {"goal":15,"content":35,"tech":35,"milestone":5,"tag":10}。
     */
    private String fieldWeights;

    /**
     * JSON 字符串，例如 {"high":80,"medium":60,"low":40}。
     */
    private String thresholds;

    private Integer topK;

    private Integer simultaneousDays;

    private Integer llmEnabled;

    private Integer localWeight;

    private Integer llmWeight;

    private Integer llmTopN;

    private Integer llmTimeoutMs;

    private Integer active;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

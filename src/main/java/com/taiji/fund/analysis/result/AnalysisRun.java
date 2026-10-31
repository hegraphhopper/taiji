package com.taiji.fund.analysis.result;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分析运行记录，对应 V4 analysis_run 表。
 */
@Data
@TableName("analysis_run")
public class AnalysisRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerType;

    private Long projectId;

    private Integer configVersion;

    private LocalDateTime analysisDate;

    private String status;

    private Integer projectCount;

    private Integer pairCount;

    private Integer llmUsed;

    private Long durationMs;

    private String message;

    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

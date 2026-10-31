package com.taiji.fund.analysis.result;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目级分析快照，对应 V4 duplicate_snapshot 表。
 */
@Data
@TableName("duplicate_snapshot")
public class DuplicateSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private Long projectId;

    private Long groupId;

    private LocalDate analysisDate;

    private BigDecimal maxScore;

    private BigDecimal avgTopScore;

    private Integer highPairCount;

    private Integer crossGroupCount;

    private Integer selfGroupCount;

    private Integer ourEarlierCount;

    private Integer peerEarlierCount;

    private Integer simultaneousCount;

    private BigDecimal riskExposure;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

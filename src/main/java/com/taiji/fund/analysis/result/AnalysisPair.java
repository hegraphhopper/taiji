package com.taiji.fund.analysis.result;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目对分析结果，对应 V4 analysis_pair 表。
 *
 * <p>project_id 恒小于 peer_project_id，无序对只存一条。direction 存中性事实：
 * A_EARLIER 表示 project_id 一方在先，B_EARLIER 表示 peer_project_id 一方在先。</p>
 */
@Data
@TableName("analysis_pair")
public class AnalysisPair {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private Long projectId;

    private Long peerProjectId;

    private BigDecimal goalScore;

    private BigDecimal contentScore;

    private BigDecimal techScore;

    private BigDecimal milestoneScore;

    private BigDecimal tagScore;

    private BigDecimal localScore;

    private BigDecimal llmScore;

    private BigDecimal totalScore;

    private String level;

    private String relation;

    private String direction;

    private Long earlierProjectId;

    private String matchedPoints;

    private String diffPoints;

    private String llmReason;

    private String llmOverlaps;

    private String llmDifferences;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

package com.taiji.fund.decision;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("decision_rule")
public class DecisionRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer priority;

    private BigDecimal minScore;

    private BigDecimal maxScore;

    private String relation;

    private Integer sameGroup;

    private String action;

    private String description;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

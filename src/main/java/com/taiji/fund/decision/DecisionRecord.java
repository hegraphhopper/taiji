package com.taiji.fund.decision;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("decision_record")
public class DecisionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long runId;

    private BigDecimal riskScore;

    private String level;

    private String recommendation;

    private String reason;

    private String similarProjects;

    private BigDecimal savingsEstimate;

    private String inputHash;

    private Long operatorId;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}

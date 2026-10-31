package com.taiji.fund.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("project_review")
public class ProjectReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long reviewerId;

    private Integer round;

    private BigDecimal score;

    private String opinion;

    private String result;

    private LocalDateTime createdAt;
}

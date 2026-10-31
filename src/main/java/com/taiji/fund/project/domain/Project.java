package com.taiji.fund.project.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String fundType;

    private Long groupId;

    private Long ownerId;

    private Long techDirectionId;

    private BigDecimal budget;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;

    private String summary;

    private String expectedOutcome;

    private BigDecimal duplicateScore;

    private String riskLevel;

    private LocalDateTime submitTime;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}

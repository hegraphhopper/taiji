package com.taiji.fund.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("project_milestone")
public class ProjectMilestone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;

    private LocalDate planStart;

    private LocalDate planEnd;

    private String deliverable;

    private Integer weight;

    private Integer sort;
}

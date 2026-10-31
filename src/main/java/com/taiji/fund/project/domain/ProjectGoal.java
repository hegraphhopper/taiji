package com.taiji.fund.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("project_goal")
public class ProjectGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String content;

    private Integer sort;
}

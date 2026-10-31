package com.taiji.fund.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("project_key_tech")
public class ProjectKeyTech {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;

    private String description;

    private String difficulty;

    private Integer sort;
}

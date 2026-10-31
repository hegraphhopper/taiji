package com.taiji.fund.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_approval_log")
public class ProjectApprovalLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String fromStatus;

    private String toStatus;

    private String action;

    private Long operatorId;

    private String comment;

    private LocalDateTime createdAt;
}

package com.taiji.fund.project.dto;

import com.taiji.fund.project.domain.ProjectApprovalLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectApprovalLogVO {

    private Long id;
    private Long projectId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private Long operatorId;
    private String comment;
    private LocalDateTime createdAt;

    public static ProjectApprovalLogVO from(ProjectApprovalLog log) {
        ProjectApprovalLogVO vo = new ProjectApprovalLogVO();
        vo.setId(log.getId());
        vo.setProjectId(log.getProjectId());
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setAction(log.getAction());
        vo.setOperatorId(log.getOperatorId());
        vo.setComment(log.getComment());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}

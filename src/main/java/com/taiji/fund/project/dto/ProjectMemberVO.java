package com.taiji.fund.project.dto;

import com.taiji.fund.project.domain.ProjectMember;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectMemberVO {

    private Long id;
    private Long projectId;
    private Long userId;
    private String memberRole;
    private BigDecimal workload;

    public static ProjectMemberVO from(ProjectMember member) {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setId(member.getId());
        vo.setProjectId(member.getProjectId());
        vo.setUserId(member.getUserId());
        vo.setMemberRole(member.getMemberRole());
        vo.setWorkload(member.getWorkload());
        return vo;
    }
}

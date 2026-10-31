package com.taiji.fund.project.dto;

import com.taiji.fund.project.domain.ProjectMilestone;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectMilestoneVO {

    private Long id;
    private Long projectId;
    private String name;
    private LocalDate planStart;
    private LocalDate planEnd;
    private String deliverable;
    private Integer weight;
    private Integer sort;

    public static ProjectMilestoneVO from(ProjectMilestone milestone) {
        ProjectMilestoneVO vo = new ProjectMilestoneVO();
        vo.setId(milestone.getId());
        vo.setProjectId(milestone.getProjectId());
        vo.setName(milestone.getName());
        vo.setPlanStart(milestone.getPlanStart());
        vo.setPlanEnd(milestone.getPlanEnd());
        vo.setDeliverable(milestone.getDeliverable());
        vo.setWeight(milestone.getWeight());
        vo.setSort(milestone.getSort());
        return vo;
    }
}

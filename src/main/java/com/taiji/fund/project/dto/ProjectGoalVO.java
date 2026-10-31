package com.taiji.fund.project.dto;

import com.taiji.fund.project.domain.ProjectGoal;
import lombok.Data;

@Data
public class ProjectGoalVO {

    private Long id;
    private Long projectId;
    private String content;
    private Integer sort;

    public static ProjectGoalVO from(ProjectGoal goal) {
        ProjectGoalVO vo = new ProjectGoalVO();
        vo.setId(goal.getId());
        vo.setProjectId(goal.getProjectId());
        vo.setContent(goal.getContent());
        vo.setSort(goal.getSort());
        return vo;
    }
}

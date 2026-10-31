package com.taiji.fund.project.dto;

import com.taiji.fund.project.domain.ProjectKeyTech;
import lombok.Data;

@Data
public class ProjectKeyTechVO {

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String difficulty;
    private Integer sort;

    public static ProjectKeyTechVO from(ProjectKeyTech tech) {
        ProjectKeyTechVO vo = new ProjectKeyTechVO();
        vo.setId(tech.getId());
        vo.setProjectId(tech.getProjectId());
        vo.setName(tech.getName());
        vo.setDescription(tech.getDescription());
        vo.setDifficulty(tech.getDifficulty());
        vo.setSort(tech.getSort());
        return vo;
    }
}

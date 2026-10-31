package com.taiji.fund.project.dto;

import com.taiji.fund.project.domain.ProjectContent;
import lombok.Data;

@Data
public class ProjectContentVO {

    private Long id;
    private Long projectId;
    private String content;
    private Integer sort;

    public static ProjectContentVO from(ProjectContent content) {
        ProjectContentVO vo = new ProjectContentVO();
        vo.setId(content.getId());
        vo.setProjectId(content.getProjectId());
        vo.setContent(content.getContent());
        vo.setSort(content.getSort());
        return vo;
    }
}

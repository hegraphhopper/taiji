package com.taiji.fund.project.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ProjectActionRequest {

    @NotBlank(message = "操作不能为空")
    private String action;

    @Size(max = 1000, message = "审批意见不能超过 1000 字")
    private String comment;

    private Integer version;
}

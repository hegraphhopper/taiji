package com.taiji.fund.system.group;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class GroupSaveRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String code;

    private Long parentId;

    @Size(max = 50)
    private String leader;

    private Integer sort;

    private Integer enabled;
}

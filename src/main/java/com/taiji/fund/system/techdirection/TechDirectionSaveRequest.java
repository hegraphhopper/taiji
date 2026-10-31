package com.taiji.fund.system.techdirection;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class TechDirectionSaveRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String code;

    private Long parentId;

    private List<String> keywords;

    private Integer sort;

    private Integer enabled;
}

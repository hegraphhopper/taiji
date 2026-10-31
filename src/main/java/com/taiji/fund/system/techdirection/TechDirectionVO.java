package com.taiji.fund.system.techdirection;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TechDirectionVO {

    private Long id;

    private String code;

    private String name;

    private Long parentId;

    private List<String> keywords;

    private Integer sort;

    private Integer enabled;
}

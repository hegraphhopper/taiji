package com.taiji.fund.system.group;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupVO {

    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private String leader;

    private Integer sort;

    private Integer enabled;
}

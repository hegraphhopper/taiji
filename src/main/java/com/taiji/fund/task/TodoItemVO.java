package com.taiji.fund.task;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TodoItemVO {

    private Long projectId;

    private String code;

    private String name;

    private Long groupId;

    private String groupName;

    private String status;

    private String action;

    private String actionLabel;
}

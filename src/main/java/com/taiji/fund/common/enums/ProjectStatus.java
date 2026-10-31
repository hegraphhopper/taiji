package com.taiji.fund.common.enums;

import lombok.Getter;

@Getter
public enum ProjectStatus {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    REVIEWING("REVIEWING", "评审中"),
    APPROVED("APPROVED", "已批准"),
    REJECTED("REJECTED", "已驳回"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已结项"),
    TERMINATED("TERMINATED", "已终止");

    private final String code;
    private final String label;

    ProjectStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}

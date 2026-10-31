package com.taiji.fund.common.enums;

import lombok.Getter;

@Getter
public enum ProjectAction {

    SUBMIT("SUBMIT", "提交"),
    WITHDRAW("WITHDRAW", "撤回"),
    ACCEPT("ACCEPT", "形式审查通过"),
    APPROVE("APPROVE", "确认立项"),
    REJECT("REJECT", "驳回"),
    START("START", "开始执行"),
    COMPLETE("COMPLETE", "结项"),
    TERMINATE("TERMINATE", "终止");

    private final String code;
    private final String label;

    ProjectAction(String code, String label) {
        this.code = code;
        this.label = label;
    }
}

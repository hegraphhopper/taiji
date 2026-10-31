package com.taiji.fund.common.enums;

import lombok.Getter;

@Getter
public enum ReviewResult {

    PASS("PASS", "通过"),
    REJECT("REJECT", "不通过"),
    REVISE("REVISE", "修改后通过");

    private final String code;
    private final String label;

    ReviewResult(String code, String label) {
        this.code = code;
        this.label = label;
    }
}

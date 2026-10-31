package com.taiji.fund.common.enums;

import lombok.Getter;

@Getter
public enum ParseStatus {

    PENDING("PENDING", "待解析"),
    PARSED("PARSED", "解析成功"),
    FAILED("FAILED", "解析失败");

    private final String code;
    private final String label;

    ParseStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}

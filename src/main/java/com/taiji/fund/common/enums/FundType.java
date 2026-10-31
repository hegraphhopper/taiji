package com.taiji.fund.common.enums;

import lombok.Getter;

@Getter
public enum FundType {

    PLATFORM("PLATFORM", "平台基金"),
    INNOVATION("INNOVATION", "创新基金"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

    FundType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}

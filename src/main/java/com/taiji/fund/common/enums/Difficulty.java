package com.taiji.fund.common.enums;

import lombok.Getter;

@Getter
public enum Difficulty {

    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String label;

    Difficulty(String code, String label) {
        this.code = code;
        this.label = label;
    }
}

package com.taiji.fund.common.enums;

import lombok.Getter;

@Getter
public enum MemberRole {

    LEADER("LEADER", "负责人"),
    CORE("CORE", "核心成员"),
    PARTICIPANT("PARTICIPANT", "参与人");

    private final String code;
    private final String label;

    MemberRole(String code, String label) {
        this.code = code;
        this.label = label;
    }
}

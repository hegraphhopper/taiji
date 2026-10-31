package com.taiji.fund.decision;

/**
 * 建议动作与风险等级常量。
 */
public final class DecisionConstants {

    public static final String MODE_PROJECT = "PROJECT";
    public static final String MODE_TEXT = "TEXT";

    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_EXPLAIN = "EXPLAIN";
    public static final String ACTION_MERGE = "MERGE";
    public static final String ACTION_TERMINATE = "TERMINATE";

    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_LOW = "LOW";
    public static final String LEVEL_NONE = "NONE";

    private DecisionConstants() {
    }
}

package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.config.Thresholds;

/**
 * 重复度等级：高/中/低/无明显重复。
 */
public enum DuplicateLevel {

    HIGH,
    MEDIUM,
    LOW,
    NONE;

    public static DuplicateLevel of(double score, Thresholds thresholds) {
        if (thresholds == null) {
            thresholds = Thresholds.defaults();
        }
        if (score >= thresholds.getHigh()) {
            return HIGH;
        }
        if (score >= thresholds.getMedium()) {
            return MEDIUM;
        }
        if (score >= thresholds.getLow()) {
            return LOW;
        }
        return NONE;
    }
}

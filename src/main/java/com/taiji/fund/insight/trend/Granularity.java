package com.taiji.fund.insight.trend;

import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;

/**
 * 趋势时间粒度。
 */
public enum Granularity {

    MONTH,
    QUARTER,
    YEAR;

    public static Granularity parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MONTH;
        }
        String normalized = value.trim().toUpperCase();
        for (Granularity granularity : values()) {
            if (granularity.name().equals(normalized)) {
                return granularity;
            }
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "granularity 仅支持 MONTH/QUARTER/YEAR");
    }
}

package com.taiji.fund.insight.trend;

import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 闭区间月份范围。
 */
@Getter
public class DateRange {

    private final YearMonth start;

    private final YearMonth end;

    public DateRange(YearMonth start, YearMonth end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate startDate() {
        return start.atDay(1);
    }

    public LocalDate endDate() {
        return end.atEndOfMonth();
    }

    public String startLabel() {
        return TrendSupport.monthLabel(start);
    }

    public String endLabel() {
        return TrendSupport.monthLabel(end);
    }
}

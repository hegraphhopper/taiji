package com.taiji.fund.insight.trend;

import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 趋势时间口径工具：月份解析、粒度分桶、边界计算与数值规整。
 *
 * <p>月度按快照 analysis_date（DATE）直接取 YearMonth，避免 JVM 默认时区差异；
 * 只有“当前月份”的默认值使用 Asia/Shanghai。</p>
 */
public final class TrendSupport {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    public static final String DEFAULT_NOTE = "归因按项目视角统计，跨组对在双方项目下各计一次；"
            + "高风险数按各次分析时的阈值判定；pairCount 为项目视角对数，uniquePairCount 为唯一项目对数。";

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final int MAX_SPAN_MONTHS = 36;

    private static final int DEFAULT_SPAN_MONTHS = 12;

    private TrendSupport() {
    }

    public static YearMonth currentMonth() {
        return YearMonth.now(ZONE);
    }

    public static String monthLabel(YearMonth month) {
        return month.format(MONTH_FORMAT);
    }

    public static YearMonth parseMonth(String value, String field, YearMonth fallback) {
        if (value == null || value.trim().isEmpty()) {
            if (fallback == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, field + " 不能为空");
            }
            return fallback;
        }
        try {
            return YearMonth.parse(value.trim(), MONTH_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, field + " 格式必须为 yyyy-MM");
        }
    }

    public static DateRange resolveRange(String startMonth, String endMonth) {
        YearMonth current = currentMonth();
        YearMonth start = parseMonth(startMonth, "startMonth", current.minusMonths(DEFAULT_SPAN_MONTHS - 1L));
        YearMonth end = parseMonth(endMonth, "endMonth", current);
        if (start.isAfter(end)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "startMonth 不能晚于 endMonth");
        }
        long span = ChronoUnit.MONTHS.between(start, end) + 1;
        if (span > MAX_SPAN_MONTHS) {
            throw new BizException(ErrorCode.BAD_REQUEST, "时间跨度不能超过 " + MAX_SPAN_MONTHS + " 个月");
        }
        return new DateRange(start, end);
    }

    public static String periodOf(LocalDate date, Granularity granularity) {
        if (date == null) {
            return null;
        }
        YearMonth month = YearMonth.from(date);
        switch (granularity) {
            case QUARTER:
                return String.format("%d-Q%d", month.getYear(), (month.getMonthValue() - 1) / 3 + 1);
            case YEAR:
                return String.valueOf(month.getYear());
            case MONTH:
            default:
                return month.format(MONTH_FORMAT);
        }
    }

    public static String periodOf(YearMonth month, Granularity granularity) {
        return periodOf(month.atDay(1), granularity);
    }

    public static LocalDate periodStart(String period, Granularity granularity) {
        if (period == null || period.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "period 不能为空");
        }
        String value = period.trim();
        try {
            switch (granularity) {
                case QUARTER: {
                    String[] parts = value.split("-Q");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("quarter");
                    }
                    int year = Integer.parseInt(parts[0]);
                    int quarter = Integer.parseInt(parts[1]);
                    if (quarter < 1 || quarter > 4) {
                        throw new IllegalArgumentException("quarter");
                    }
                    return YearMonth.of(year, (quarter - 1) * 3 + 1).atDay(1);
                }
                case YEAR:
                    return YearMonth.of(Integer.parseInt(value), 1).atDay(1);
                case MONTH:
                default:
                    return YearMonth.parse(value, MONTH_FORMAT).atDay(1);
            }
        } catch (RuntimeException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "period 与 granularity 不匹配");
        }
    }

    public static BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal ratio(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP);
    }
}

package com.taiji.fund.analysis.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 重复度等级阈值：0 &lt; low &lt; medium &lt; high ≤ 100。
 */
public final class Thresholds {

    public static final String LOW = "low";
    public static final String MEDIUM = "medium";
    public static final String HIGH = "high";

    private final int low;
    private final int medium;
    private final int high;

    public Thresholds(int low, int medium, int high) {
        this.low = low;
        this.medium = medium;
        this.high = high;
    }

    public static Thresholds defaults() {
        return new Thresholds(40, 60, 80);
    }

    public static Thresholds of(Map<String, Integer> map) {
        return new Thresholds(value(map, LOW), value(map, MEDIUM), value(map, HIGH));
    }

    public static boolean containsAllKeys(Map<String, Integer> map) {
        return map != null
                && map.get(LOW) != null
                && map.get(MEDIUM) != null
                && map.get(HIGH) != null;
    }

    private static int value(Map<String, Integer> map, String key) {
        Integer value = map == null ? null : map.get(key);
        return value == null ? 0 : value;
    }

    public boolean isValid() {
        return low > 0 && low < medium && medium < high && high <= 100;
    }

    public int getLow() {
        return low;
    }

    public int getMedium() {
        return medium;
    }

    public int getHigh() {
        return high;
    }

    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(HIGH, high);
        map.put(MEDIUM, medium);
        map.put(LOW, low);
        return map;
    }
}

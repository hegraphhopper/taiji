package com.taiji.fund.analysis.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 五维字段权重（goal/content/tech/milestone/tag）。
 *
 * <p>数据库中以 JSON 字符串保存，禁止使用数据库 JSON 列类型。</p>
 */
public final class FieldWeights {

    public static final String GOAL = "goal";
    public static final String CONTENT = "content";
    public static final String TECH = "tech";
    public static final String MILESTONE = "milestone";
    public static final String TAG = "tag";

    private final int goal;
    private final int content;
    private final int tech;
    private final int milestone;
    private final int tag;

    public FieldWeights(int goal, int content, int tech, int milestone, int tag) {
        this.goal = goal;
        this.content = content;
        this.tech = tech;
        this.milestone = milestone;
        this.tag = tag;
    }

    public static FieldWeights defaults() {
        return new FieldWeights(15, 35, 35, 5, 10);
    }

    /**
     * 缺失键按 0 处理，键是否齐全由配置服务校验。
     */
    public static FieldWeights of(Map<String, Integer> map) {
        return new FieldWeights(value(map, GOAL), value(map, CONTENT), value(map, TECH),
                value(map, MILESTONE), value(map, TAG));
    }

    public static boolean containsAllKeys(Map<String, Integer> map) {
        return map != null
                && map.get(GOAL) != null
                && map.get(CONTENT) != null
                && map.get(TECH) != null
                && map.get(MILESTONE) != null
                && map.get(TAG) != null;
    }

    private static int value(Map<String, Integer> map, String key) {
        Integer value = map == null ? null : map.get(key);
        return value == null ? 0 : value;
    }

    public int get(String dimension) {
        if (GOAL.equals(dimension)) {
            return goal;
        }
        if (CONTENT.equals(dimension)) {
            return content;
        }
        if (TECH.equals(dimension)) {
            return tech;
        }
        if (MILESTONE.equals(dimension)) {
            return milestone;
        }
        if (TAG.equals(dimension)) {
            return tag;
        }
        return 0;
    }

    public int getGoal() {
        return goal;
    }

    public int getContent() {
        return content;
    }

    public int getTech() {
        return tech;
    }

    public int getMilestone() {
        return milestone;
    }

    public int getTag() {
        return tag;
    }

    public int total() {
        return goal + content + tech + milestone + tag;
    }

    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(GOAL, goal);
        map.put(CONTENT, content);
        map.put(TECH, tech);
        map.put(MILESTONE, milestone);
        map.put(TAG, tag);
        return map;
    }
}

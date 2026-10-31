package com.taiji.fund.analysis.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 稀疏向量（token → 权重），L2 归一化，支持点积。
 */
public final class SparseVector {

    private final Map<String, Double> weights;
    private final double norm;

    private SparseVector(Map<String, Double> weights) {
        this.weights = Collections.unmodifiableMap(weights);
        double sum = 0D;
        for (double value : weights.values()) {
            sum += value * value;
        }
        this.norm = Math.sqrt(sum);
    }

    public static SparseVector empty() {
        return new SparseVector(Collections.emptyMap());
    }

    /**
     * 由原始权重构建并执行 L2 归一化。
     */
    public static SparseVector of(Map<String, Double> rawWeights) {
        if (rawWeights == null || rawWeights.isEmpty()) {
            return empty();
        }
        double sum = 0D;
        for (double value : rawWeights.values()) {
            sum += value * value;
        }
        if (sum <= 0D) {
            return empty();
        }
        double length = Math.sqrt(sum);
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : rawWeights.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(entry.getKey(), entry.getValue() / length);
        }
        return new SparseVector(normalized);
    }

    public double get(String token) {
        Double value = weights.get(token);
        return value == null ? 0D : value;
    }

    public double dot(SparseVector other) {
        if (other == null || weights.isEmpty() || other.weights.isEmpty()) {
            return 0D;
        }
        SparseVector smaller = weights.size() <= other.weights.size() ? this : other;
        SparseVector larger = smaller == this ? other : this;
        double result = 0D;
        for (Map.Entry<String, Double> entry : smaller.weights.entrySet()) {
            result += entry.getValue() * larger.get(entry.getKey());
        }
        return result;
    }

    public double norm() {
        return norm;
    }

    public boolean isEmpty() {
        return weights.isEmpty();
    }

    public int size() {
        return weights.size();
    }

    public Set<String> tokens() {
        return weights.keySet();
    }

    public Map<String, Double> asMap() {
        return weights;
    }

    public List<Map.Entry<String, Double>> topTokens(int limit) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(weights.entrySet());
        entries.sort((left, right) -> {
            int compared = Double.compare(right.getValue(), left.getValue());
            return compared != 0 ? compared : left.getKey().compareTo(right.getKey());
        });
        return entries.size() <= limit ? entries : new ArrayList<>(entries.subList(0, limit));
    }
}

package com.taiji.fund.analysis.vector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Jaccard 相似度。空集与空集返回 0（避免无数据时被判定为完全重复）。
 */
public final class JaccardSimilarity {

    private JaccardSimilarity() {
    }

    public static double jaccard(Set<String> left, Set<String> right) {
        Set<String> safeLeft = left == null ? Collections.emptySet() : left;
        Set<String> safeRight = right == null ? Collections.emptySet() : right;
        if (safeLeft.isEmpty() || safeRight.isEmpty()) {
            return 0D;
        }
        Set<String> intersection = new HashSet<>(safeLeft);
        intersection.retainAll(safeRight);
        Set<String> union = new HashSet<>(safeLeft);
        union.addAll(safeRight);
        if (union.isEmpty()) {
            return 0D;
        }
        return (double) intersection.size() / union.size();
    }
}

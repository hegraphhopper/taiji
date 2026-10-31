package com.taiji.fund.analysis.vector;

/**
 * 余弦相似度。空向量返回 0。
 */
public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    public static double cosine(SparseVector left, SparseVector right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0D;
        }
        double denominator = left.norm() * right.norm();
        if (denominator <= 0D) {
            return 0D;
        }
        double value = left.dot(right) / denominator;
        if (value < 0D) {
            return 0D;
        }
        return value > 1D ? 1D : value;
    }
}

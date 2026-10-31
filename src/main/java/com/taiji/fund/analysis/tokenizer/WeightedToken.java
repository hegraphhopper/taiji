package com.taiji.fund.analysis.tokenizer;

import lombok.ToString;

import java.util.Objects;

/**
 * 带权 token：普通 2-gram/英文词权重 1，术语命中权重 3。
 */
@ToString
public final class WeightedToken {

    private final String token;
    private final int weight;

    public WeightedToken(String token, int weight) {
        this.token = token;
        this.weight = weight;
    }

    public String getToken() {
        return token;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WeightedToken)) {
            return false;
        }
        WeightedToken that = (WeightedToken) other;
        return weight == that.weight && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, weight);
    }
}

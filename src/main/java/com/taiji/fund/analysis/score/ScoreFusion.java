package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.config.FieldWeights;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;

/**
 * 权重归一化融合。
 *
 * <p>localScore = Σ(维度分 × 权重) / Σ(有文本维度的权重)；结果保留 2 位小数。</p>
 */
public class ScoreFusion {

    private static final String[] DIMENSIONS = {
            FieldWeights.GOAL, FieldWeights.CONTENT, FieldWeights.TECH,
            FieldWeights.MILESTONE, FieldWeights.TAG
    };

    public double fuse(DimensionScores scores, FieldWeights weights) {
        if (scores == null || weights == null) {
            throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, "字段权重配置缺失");
        }
        double weightedSum = 0D;
        double weightSum = 0D;
        for (String dimension : DIMENSIONS) {
            if (scores.isMissing(dimension)) {
                continue;
            }
            int weight = weights.get(dimension);
            if (weight < 0) {
                throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, "字段权重不能为负数");
            }
            weightedSum += scores.score(dimension) * weight;
            weightSum += weight;
        }
        if (weightSum <= 0D) {
            throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, "有效字段权重之和必须大于 0");
        }
        return clamp(weightedSum / weightSum);
    }

    /**
     * 本地分与大模型分融合；llmScore 为空时保留本地分。
     */
    public double fuse(double localScore, Double llmScore, int localWeight, int llmWeight) {
        if (llmScore == null) {
            return clamp(localScore);
        }
        if (localWeight < 0 || llmWeight < 0 || localWeight + llmWeight <= 0) {
            throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, "本地权重与大模型权重之和必须大于 0");
        }
        double fused = (localWeight * localScore + llmWeight * llmScore) / (localWeight + llmWeight);
        return clamp(fused);
    }

    private double clamp(double value) {
        double bounded = Math.max(0D, Math.min(100D, value));
        return Math.round(bounded * 100D) / 100D;
    }
}

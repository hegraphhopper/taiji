package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.config.FieldWeights;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 五个维度的得分（0-100）与文本缺失标记。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionScores {

    private double goalScore;

    private double contentScore;

    private double techScore;

    private double milestoneScore;

    private double tagScore;

    private boolean goalMissing;

    private boolean contentMissing;

    private boolean techMissing;

    private boolean milestoneMissing;

    private boolean tagMissing;

    public double score(String dimension) {
        if (FieldWeights.GOAL.equals(dimension)) {
            return goalScore;
        }
        if (FieldWeights.CONTENT.equals(dimension)) {
            return contentScore;
        }
        if (FieldWeights.TECH.equals(dimension)) {
            return techScore;
        }
        if (FieldWeights.MILESTONE.equals(dimension)) {
            return milestoneScore;
        }
        if (FieldWeights.TAG.equals(dimension)) {
            return tagScore;
        }
        return 0D;
    }

    public boolean isMissing(String dimension) {
        if (FieldWeights.GOAL.equals(dimension)) {
            return goalMissing;
        }
        if (FieldWeights.CONTENT.equals(dimension)) {
            return contentMissing;
        }
        if (FieldWeights.TECH.equals(dimension)) {
            return techMissing;
        }
        if (FieldWeights.MILESTONE.equals(dimension)) {
            return milestoneMissing;
        }
        if (FieldWeights.TAG.equals(dimension)) {
            return tagMissing;
        }
        return true;
    }
}

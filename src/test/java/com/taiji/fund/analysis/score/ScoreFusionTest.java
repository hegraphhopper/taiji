package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.config.FieldWeights;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreFusionTest {

    private final ScoreFusion fusion = new ScoreFusion();

    @Test
    @DisplayName("UT-12 按字段权重归一化融合")
    void ut12_weightedFusion() {
        DimensionScores scores = DimensionScores.builder()
                .goalScore(100D)
                .contentScore(50D)
                .techScore(0D)
                .milestoneScore(50D)
                .tagScore(100D)
                .build();

        assertThat(fusion.fuse(scores, FieldWeights.defaults())).isCloseTo(45D, within(0.01));
    }

    @Test
    @DisplayName("UT-12 仅保留内容权重时总分等于内容维度分")
    void ut12_singleDimensionWeight() {
        DimensionScores scores = DimensionScores.builder()
                .goalScore(10D)
                .contentScore(73.25D)
                .techScore(20D)
                .milestoneScore(30D)
                .tagScore(40D)
                .build();

        assertThat(fusion.fuse(scores, new FieldWeights(0, 100, 0, 0, 0)))
                .isCloseTo(73.25D, within(0.01));
    }

    @Test
    @DisplayName("UT-12 权重合计为 0 抛业务异常而不是除零")
    void ut12_zeroWeightRejected() {
        DimensionScores scores = DimensionScores.builder().goalScore(100D).build();
        BizException exception = assertThrows(BizException.class,
                () -> fusion.fuse(scores, new FieldWeights(0, 0, 0, 0, 0)));
        assertThat(exception.getCode()).isEqualTo(ErrorCode.ANALYSIS_WEIGHT_INVALID.getCode());
    }

    @Test
    @DisplayName("UT-12 全部维度缺失时抛业务异常")
    void ut12_allMissingRejected() {
        DimensionScores scores = DimensionScores.builder()
                .goalMissing(true)
                .contentMissing(true)
                .techMissing(true)
                .milestoneMissing(true)
                .tagMissing(true)
                .build();
        assertThrows(BizException.class, () -> fusion.fuse(scores, FieldWeights.defaults()));
    }

    @Test
    @DisplayName("UT-20 本地分与大模型分按 70/30 融合，llm 为空保留本地分")
    void ut20_llmFusion() {
        assertThat(fusion.fuse(70D, 90D, 70, 30)).isCloseTo(76D, within(0.01));
        assertThat(fusion.fuse(64.37D, null, 70, 30)).isCloseTo(64.37D, within(0.01));
        assertThat(fusion.fuse(64.37D, 90D, 100, 0)).isCloseTo(64.37D, within(0.01));
    }
}

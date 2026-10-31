package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.config.FieldWeights;
import com.taiji.fund.analysis.support.MilestoneInfo;
import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import com.taiji.fund.analysis.tokenizer.ChineseTokenizer;
import com.taiji.fund.analysis.tokenizer.WeightedToken;
import com.taiji.fund.analysis.vector.SparseVector;
import com.taiji.fund.analysis.vector.TfIdfVectorizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DimensionScorerTest {

    private final DimensionScorer scorer = new DimensionScorer();

    @Test
    @DisplayName("UT-13 阶段名与时间区间完全一致得 100")
    void ut13_fullMilestoneMatch() {
        List<MilestoneInfo> first = Arrays.asList(
                new MilestoneInfo("原型开发", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), "原型"),
                new MilestoneInfo("集成测试", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 1), "报告"));
        List<MilestoneInfo> second = Arrays.asList(
                new MilestoneInfo("原型开发", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), "原型"),
                new MilestoneInfo("集成测试", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 1), "报告"));

        assertThat(scorer.milestoneScore(first, second)).isCloseTo(100D, within(0.01));
    }

    @Test
    @DisplayName("UT-13 一方无日期时仅按阶段名 Jaccard 计分")
    void ut13_namesOnlyWhenDatesMissing() {
        List<MilestoneInfo> first = Collections.singletonList(
                new MilestoneInfo("原型开发", null, null, "原型"));
        List<MilestoneInfo> second = Collections.singletonList(
                new MilestoneInfo("原型开发", null, null, "原型"));

        assertThat(scorer.milestoneScore(first, second)).isCloseTo(60D, within(0.01));
    }

    @Test
    @DisplayName("UT-13 阶段名无交集且无日期得 0")
    void ut13_unrelatedNames() {
        List<MilestoneInfo> first = Collections.singletonList(new MilestoneInfo("原型开发", null, null, null));
        List<MilestoneInfo> second = Collections.singletonList(new MilestoneInfo("市场推广", null, null, null));

        assertThat(scorer.milestoneScore(first, second)).isZero();
    }

    @Test
    @DisplayName("UT-13 部分阶段名与区间重叠落在合理区间")
    void ut13_partialMilestone() {
        List<MilestoneInfo> first = Arrays.asList(
                new MilestoneInfo("a", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), null),
                new MilestoneInfo("b", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 6, 1), null));
        List<MilestoneInfo> second = Arrays.asList(
                new MilestoneInfo("b", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 1), null),
                new MilestoneInfo("c", LocalDate.of(2026, 4, 2), LocalDate.of(2026, 7, 1), null));

        double score = scorer.milestoneScore(first, second);
        assertThat(score).isGreaterThan(0D).isLessThan(50D);
    }

    @Test
    @DisplayName("UT-13 标签 Jaccard 计分")
    void ut13_tagJaccard() {
        assertThat(scorer.tagScore(set("a", "b"), set("b", "c"))).isCloseTo(33.33, within(0.01));
        assertThat(scorer.tagScore(set("a"), set("a"))).isCloseTo(100D, within(0.01));
    }

    @Test
    @DisplayName("UT-08 空维度得 0 且总分按剩余维度归一化")
    void ut08_missingDimensionExcluded() {
        ProjectAnalysisInput target = ProjectAnalysisInput.builder()
                .projectId(1L)
                .goalText("")
                .contentText("深度学习模型压缩")
                .keyTechText("")
                .milestones(Collections.singletonList(new MilestoneInfo("原型开发", null, null, null)))
                .tags(set("人工智能"))
                .build();
        ProjectAnalysisInput peer = ProjectAnalysisInput.builder()
                .projectId(2L)
                .goalText("完全不相关的目标")
                .contentText("深度学习模型压缩")
                .keyTechText("")
                .milestones(Collections.singletonList(new MilestoneInfo("原型开发", null, null, null)))
                .tags(set("人工智能"))
                .build();

        List<List<WeightedToken>> contentCorpus = Arrays.asList(
                ChineseTokenizer.tokenize(target.getContentText()),
                ChineseTokenizer.tokenize(peer.getContentText()));
        List<SparseVector> vectors = new TfIdfVectorizer().vectorize(contentCorpus);
        SparseVector empty = SparseVector.empty();

        DimensionScores scores = scorer.score(target, empty, vectors.get(0), empty,
                peer, empty, vectors.get(1), empty);

        assertThat(scores.isGoalMissing()).isTrue();
        assertThat(scores.getGoalScore()).isZero();
        assertThat(scores.isContentMissing()).isFalse();
        assertThat(scores.getContentScore()).isCloseTo(100D, within(0.01));
        assertThat(scores.getMilestoneScore()).isCloseTo(60D, within(0.01));
        assertThat(scores.getTagScore()).isCloseTo(100D, within(0.01));

        double fused = new ScoreFusion().fuse(scores, new FieldWeights(0, 100, 0, 0, 0));
        assertThat(fused).isCloseTo(100D, within(0.01));
    }

    private LinkedHashSet<String> set(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}

package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.config.FieldWeights;
import com.taiji.fund.analysis.support.MilestoneInfo;
import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import com.taiji.fund.analysis.tokenizer.TextNormalizer;
import com.taiji.fund.analysis.vector.CosineSimilarity;
import com.taiji.fund.analysis.vector.JaccardSimilarity;
import com.taiji.fund.analysis.vector.SparseVector;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 五维打分：goal/content/tech 余弦 ×100；里程碑 = 名称 Jaccard×0.6 + 区间重叠×0.4；tag = Jaccard×100。
 */
public class DimensionScorer {

    private static final double NAME_WEIGHT = 0.6D;
    private static final double TIME_WEIGHT = 0.4D;

    public DimensionScores score(ProjectAnalysisInput target,
                                 SparseVector goalTarget, SparseVector contentTarget, SparseVector techTarget,
                                 ProjectAnalysisInput peer,
                                 SparseVector goalPeer, SparseVector contentPeer, SparseVector techPeer) {
        boolean goalMissing = !target.hasText(FieldWeights.GOAL) || !peer.hasText(FieldWeights.GOAL);
        boolean contentMissing = !target.hasText(FieldWeights.CONTENT) || !peer.hasText(FieldWeights.CONTENT);
        boolean techMissing = !target.hasText(FieldWeights.TECH) || !peer.hasText(FieldWeights.TECH);
        boolean milestoneMissing = target.getMilestones().isEmpty() || peer.getMilestones().isEmpty();
        boolean tagMissing = target.getTags().isEmpty() || peer.getTags().isEmpty();

        return DimensionScores.builder()
                .goalScore(goalMissing ? 0D : textScore(goalTarget, goalPeer))
                .contentScore(contentMissing ? 0D : textScore(contentTarget, contentPeer))
                .techScore(techMissing ? 0D : textScore(techTarget, techPeer))
                .milestoneScore(milestoneMissing ? 0D
                        : milestoneScore(target.getMilestones(), peer.getMilestones()))
                .tagScore(tagMissing ? 0D : tagScore(target.getTags(), peer.getTags()))
                .goalMissing(goalMissing)
                .contentMissing(contentMissing)
                .techMissing(techMissing)
                .milestoneMissing(milestoneMissing)
                .tagMissing(tagMissing)
                .build();
    }

    public double textScore(SparseVector target, SparseVector peer) {
        return round2(CosineSimilarity.cosine(target, peer) * 100D);
    }

    public double milestoneScore(List<MilestoneInfo> target, List<MilestoneInfo> peer) {
        if (target == null || peer == null || target.isEmpty() || peer.isEmpty()) {
            return 0D;
        }
        Set<String> targetNames = names(target);
        Set<String> peerNames = names(peer);
        double nameScore = JaccardSimilarity.jaccard(targetNames, peerNames);
        double timeScore = timeOverlap(target, peer);
        return round2((nameScore * NAME_WEIGHT + timeScore * TIME_WEIGHT) * 100D);
    }

    public double tagScore(Set<String> target, Set<String> peer) {
        return round2(JaccardSimilarity.jaccard(target, peer) * 100D);
    }

    private Set<String> names(List<MilestoneInfo> milestones) {
        Set<String> names = new LinkedHashSet<>();
        for (MilestoneInfo milestone : milestones) {
            if (milestone == null || milestone.getName() == null) {
                continue;
            }
            String name = TextNormalizer.normalize(milestone.getName());
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private double timeOverlap(List<MilestoneInfo> target, List<MilestoneInfo> peer) {
        long[] targetRange = interval(target);
        long[] peerRange = interval(peer);
        if (targetRange == null || peerRange == null) {
            return 0D;
        }
        long targetLength = targetRange[1] - targetRange[0] + 1;
        long peerLength = peerRange[1] - peerRange[0] + 1;
        long intersectionStart = Math.max(targetRange[0], peerRange[0]);
        long intersectionEnd = Math.min(targetRange[1], peerRange[1]);
        long intersection = intersectionEnd >= intersectionStart
                ? intersectionEnd - intersectionStart + 1 : 0L;
        long union = targetLength + peerLength - intersection;
        if (union <= 0L) {
            return 0D;
        }
        return (double) intersection / union;
    }

    private long[] interval(List<MilestoneInfo> milestones) {
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        boolean found = false;
        for (MilestoneInfo milestone : milestones) {
            if (milestone == null || !milestone.hasPlanDate()) {
                continue;
            }
            LocalDate planStart = milestone.getPlanStart();
            LocalDate planEnd = milestone.getPlanEnd();
            if (planEnd.isBefore(planStart)) {
                continue;
            }
            start = Math.min(start, planStart.toEpochDay());
            end = Math.max(end, planEnd.toEpochDay());
            found = true;
        }
        if (!found) {
            return null;
        }
        return new long[]{start, end};
    }

    static double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }
}

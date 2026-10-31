package com.taiji.fund.analysis.run;

import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 单次运行的计算产物：项目对、快照、项目分回写与统计计数。
 */
@Getter
@Builder
public class AnalysisBatch {

    private final int projectCount;

    private final int pairCount;

    private final boolean llmUsed;

    private final boolean writeProjectScores;

    private final List<AnalysisPair> pairs;

    private final List<DuplicateSnapshot> snapshots;

    private final List<ProjectScoreUpdate> projectScores;

    public List<AnalysisPair> getPairs() {
        return pairs == null ? Collections.emptyList() : pairs;
    }

    public List<DuplicateSnapshot> getSnapshots() {
        return snapshots == null ? Collections.emptyList() : snapshots;
    }

    public List<ProjectScoreUpdate> getProjectScores() {
        return projectScores == null ? Collections.emptyList() : projectScores;
    }

    public static AnalysisBatch empty() {
        return AnalysisBatch.builder()
                .projectCount(0)
                .pairCount(0)
                .llmUsed(false)
                .writeProjectScores(true)
                .build();
    }
}

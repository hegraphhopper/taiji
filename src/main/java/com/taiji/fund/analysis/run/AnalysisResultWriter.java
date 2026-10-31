package com.taiji.fund.analysis.run;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.AnalysisPairMapper;
import com.taiji.fund.analysis.result.AnalysisRunMapper;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.DuplicateSnapshotMapper;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分析结果落库：项目对 + 快照 + 项目分回写在同一事务内完成，任一步失败整体回滚。
 */
@Service
@RequiredArgsConstructor
public class AnalysisResultWriter {

    private static final int BATCH_SIZE = 500;

    private final AnalysisRunMapper runMapper;
    private final AnalysisPairMapper pairMapper;
    private final DuplicateSnapshotMapper snapshotMapper;
    private final ProjectMapper projectMapper;

    @Transactional
    public void persist(AnalysisBatch batch) {
        if (batch == null) {
            return;
        }
        writePairs(batch.getPairs());
        writeSnapshots(batch.getSnapshots());
        if (batch.isWriteProjectScores()) {
            writeProjectScores(batch.getProjectScores());
        }
    }

    /**
     * 清理指定运行的明细（项目对与快照），用于失败回滚。
     */
    @Transactional
    public void deleteDetails(Long runId) {
        if (runId == null) {
            return;
        }
        pairMapper.delete(Wrappers.<AnalysisPair>lambdaQuery().eq(AnalysisPair::getRunId, runId));
        snapshotMapper.delete(Wrappers.<DuplicateSnapshot>lambdaQuery()
                .eq(DuplicateSnapshot::getRunId, runId));
    }

    /**
     * 级联删除运行：先删明细再删运行，单事务。
     */
    @Transactional
    public void deleteRunCascade(Long runId) {
        if (runId == null) {
            return;
        }
        pairMapper.delete(Wrappers.<AnalysisPair>lambdaQuery().eq(AnalysisPair::getRunId, runId));
        snapshotMapper.delete(Wrappers.<DuplicateSnapshot>lambdaQuery()
                .eq(DuplicateSnapshot::getRunId, runId));
        runMapper.deleteById(runId);
    }

    private void writePairs(List<AnalysisPair> pairs) {
        int size = pairs.size();
        for (int start = 0; start < size; start += BATCH_SIZE) {
            int end = Math.min(size, start + BATCH_SIZE);
            for (int index = start; index < end; index++) {
                pairMapper.insert(pairs.get(index));
            }
        }
    }

    private void writeSnapshots(List<DuplicateSnapshot> snapshots) {
        for (DuplicateSnapshot snapshot : snapshots) {
            snapshotMapper.insert(snapshot);
        }
    }

    private void writeProjectScores(List<ProjectScoreUpdate> updates) {
        for (ProjectScoreUpdate update : updates) {
            if (update.getProjectId() == null) {
                continue;
            }
            Project project = new Project();
            project.setId(update.getProjectId());
            project.setDuplicateScore(update.getMaxScore());
            project.setRiskLevel(update.getLevel());
            projectMapper.updateById(project);
        }
    }
}

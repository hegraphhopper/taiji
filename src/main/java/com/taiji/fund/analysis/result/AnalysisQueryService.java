package com.taiji.fund.analysis.result;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.config.AnalysisConfigService;
import com.taiji.fund.analysis.config.AnalysisConfigVO;
import com.taiji.fund.analysis.config.Thresholds;
import com.taiji.fund.analysis.result.dto.AnalysisProjectBriefVO;
import com.taiji.fund.analysis.result.dto.DuplicateSnapshotVO;
import com.taiji.fund.analysis.result.dto.PairDetailVO;
import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import com.taiji.fund.analysis.result.dto.ProjectAnalysisReportVO;
import com.taiji.fund.analysis.score.DuplicateLevel;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 分析结果查询：项目报告、Top-K 相似项目、项目对明细。数据范围在服务层强制校验。
 */
@Service
@RequiredArgsConstructor
public class AnalysisQueryService {

    private static final int DEFAULT_TOP_K = 5;

    private final ProjectMapper projectMapper;
    private final AnalysisPairMapper pairMapper;
    private final DuplicateSnapshotMapper snapshotMapper;
    private final AnalysisRunMapper runMapper;
    private final ProjectBriefAssembler briefAssembler;
    private final AnalysisPairPresenter presenter;
    private final PeerMatchAssembler peerMatchAssembler;
    private final AnalysisConfigService configService;
    private final ProjectAccessService accessService;

    @Transactional(readOnly = true)
    public ProjectAnalysisReportVO report(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        accessService.assertCanView(project);

        AnalysisConfigVO config = configService.current();
        Thresholds thresholds = resolveThresholds(config);
        int topK = config.getTopK() == null || config.getTopK() <= 0 ? DEFAULT_TOP_K : config.getTopK();
        DuplicateSnapshot snapshot = latestSnapshot(projectId);

        List<PeerMatchVO> topPeers = new ArrayList<>();
        if (snapshot != null) {
            List<AnalysisPair> pairs = pairMapper.selectList(Wrappers.<AnalysisPair>lambdaQuery()
                    .eq(AnalysisPair::getRunId, snapshot.getRunId())
                    .and(wrapper -> wrapper.eq(AnalysisPair::getProjectId, projectId)
                            .or().eq(AnalysisPair::getPeerProjectId, projectId))
                    .orderByDesc(AnalysisPair::getTotalScore)
                    .orderByAsc(AnalysisPair::getId));
            if (pairs.size() > topK) {
                pairs = new ArrayList<>(pairs.subList(0, topK));
            }
            topPeers = peerMatchAssembler.assemble(pairs, projectId, false);
        }

        AnalysisProjectBriefVO brief = briefAssembler.assemble(project);
        String riskLevel = snapshot == null ? null
                : DuplicateLevel.of(toDouble(snapshot.getMaxScore()), thresholds).name();
        return ProjectAnalysisReportVO.builder()
                .project(brief)
                .snapshot(toSnapshotVO(snapshot))
                .topPeers(topPeers)
                .riskLevel(riskLevel)
                .thresholds(thresholds.toMap())
                .analyzed(snapshot != null)
                .build();
    }

    @Transactional(readOnly = true)
    public PairDetailVO pairDetail(Long pairId) {
        AnalysisPair pair = pairMapper.selectById(pairId);
        if (pair == null) {
            throw new BizException(ErrorCode.ANALYSIS_PAIR_NOT_FOUND);
        }
        Project first = projectMapper.selectById(pair.getProjectId());
        Project second = projectMapper.selectById(pair.getPeerProjectId());
        if (first == null || second == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        boolean firstVisible = accessService.canView(first);
        boolean secondVisible = accessService.canView(second);
        if (!firstVisible && !secondVisible) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        Long viewerProjectId = firstVisible ? first.getId() : second.getId();
        Map<Long, AnalysisProjectBriefVO> briefs =
                briefAssembler.assemble(Arrays.asList(first, second));
        PairDetailVO detail = presenter.toPairDetail(pair, viewerProjectId,
                briefs.get(first.getId()), briefs.get(second.getId()));
        AnalysisRun run = runMapper.selectById(pair.getRunId());
        if (run != null && run.getAnalysisDate() != null) {
            detail.setAnalysisDate(run.getAnalysisDate().toLocalDate());
        }
        return detail;
    }

    private DuplicateSnapshot latestSnapshot(Long projectId) {
        List<DuplicateSnapshot> snapshots = snapshotMapper.selectList(
                Wrappers.<DuplicateSnapshot>lambdaQuery()
                        .eq(DuplicateSnapshot::getProjectId, projectId)
                        .orderByDesc(DuplicateSnapshot::getAnalysisDate)
                        .orderByDesc(DuplicateSnapshot::getId));
        return snapshots.isEmpty() ? null : snapshots.get(0);
    }

    private DuplicateSnapshotVO toSnapshotVO(DuplicateSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return DuplicateSnapshotVO.builder()
                .id(snapshot.getId())
                .runId(snapshot.getRunId())
                .projectId(snapshot.getProjectId())
                .groupId(snapshot.getGroupId())
                .analysisDate(snapshot.getAnalysisDate())
                .maxScore(snapshot.getMaxScore())
                .avgTopScore(snapshot.getAvgTopScore())
                .highPairCount(snapshot.getHighPairCount())
                .crossGroupCount(snapshot.getCrossGroupCount())
                .selfGroupCount(snapshot.getSelfGroupCount())
                .ourEarlierCount(snapshot.getOurEarlierCount())
                .peerEarlierCount(snapshot.getPeerEarlierCount())
                .simultaneousCount(snapshot.getSimultaneousCount())
                .riskExposure(snapshot.getRiskExposure())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }

    private Thresholds resolveThresholds(AnalysisConfigVO config) {
        Map<String, Integer> map = config.getThresholds();
        if (!Thresholds.containsAllKeys(map)) {
            return Thresholds.defaults();
        }
        Thresholds thresholds = Thresholds.of(map);
        return thresholds.isValid() ? thresholds : Thresholds.defaults();
    }

    private double toDouble(java.math.BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }
}

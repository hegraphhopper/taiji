package com.taiji.fund.analysis.result.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 项目重复度报告：项目信息 + 最新快照 + Top-K 相似项目。
 */
@Data
@Builder
public class ProjectAnalysisReportVO {

    private AnalysisProjectBriefVO project;

    private DuplicateSnapshotVO snapshot;

    private List<PeerMatchVO> topPeers;

    private String riskLevel;

    private Map<String, Integer> thresholds;

    /**
     * 无快照时为 true，前端展示「尚未分析」。
     */
    private boolean analyzed;
}

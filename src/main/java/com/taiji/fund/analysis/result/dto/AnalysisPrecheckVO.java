package com.taiji.fund.analysis.result.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 预检结果：Top-K 相似项目 + 分维度分 + 建议动作；不写项目分。
 */
@Data
@Builder
public class AnalysisPrecheckVO {

    private Long runId;

    private Long projectId;

    private Integer peerCount;

    private Integer configVersion;

    private Boolean llmUsed;

    private List<PeerMatchVO> topPeers;
}

package com.taiji.fund.analysis.result;

import com.taiji.fund.analysis.result.dto.AnalysisProjectBriefVO;
import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把项目对转换为当前查看项目视角的 Top-K 条目，预检与项目报告共用。
 */
@Component
@RequiredArgsConstructor
public class PeerMatchAssembler {

    private final ProjectBriefAssembler briefAssembler;
    private final AnalysisPairPresenter presenter;

    public List<PeerMatchVO> assemble(List<AnalysisPair> pairs, Long viewerProjectId,
                                      boolean withSuggestion) {
        if (pairs == null || pairs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> peerIds = new LinkedHashSet<>();
        for (AnalysisPair pair : pairs) {
            Long peerId = presenter.viewPeerId(pair, viewerProjectId);
            if (peerId != null) {
                peerIds.add(peerId);
            }
        }
        Map<Long, AnalysisProjectBriefVO> briefs = briefAssembler.assembleByIds(peerIds);
        List<PeerMatchVO> result = new ArrayList<>(pairs.size());
        for (AnalysisPair pair : pairs) {
            Long peerId = presenter.viewPeerId(pair, viewerProjectId);
            String suggestion = withSuggestion ? presenter.suggestionOf(pair.getLevel()) : null;
            result.add(presenter.toPeerMatch(pair, viewerProjectId, briefs.get(peerId), suggestion));
        }
        return result;
    }
}

package com.taiji.fund.analysis.run;

import com.taiji.fund.analysis.result.AnalysisPair;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 预检结果：Top-K 项目对与参与比较的项目数量。
 */
@Getter
@AllArgsConstructor
public class PrecheckOutcome {

    private final List<AnalysisPair> pairs;

    private final int projectCount;

    private final boolean llmUsed;

    public List<AnalysisPair> getPairs() {
        return pairs == null ? Collections.emptyList() : pairs;
    }
}

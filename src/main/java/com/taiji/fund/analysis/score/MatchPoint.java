package com.taiji.fund.analysis.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 可解释片段：维度 + Top 词 + 原文片段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPoint {

    private String dimension;

    private List<String> terms;

    private String snippet;
}

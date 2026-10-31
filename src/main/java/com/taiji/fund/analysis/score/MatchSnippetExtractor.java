package com.taiji.fund.analysis.score;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiji.fund.analysis.config.FieldWeights;
import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import com.taiji.fund.analysis.vector.SparseVector;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 匹配片段与差异点提取。
 *
 * <p>matchedPoints：本方 Top 词中在对方出现的词；diffPoints：本方高频但对方缺失的词。
 * 片段回原文定位首次出现位置，前后各 60 字，总长不超过 200 字。</p>
 */
@Slf4j
public class MatchSnippetExtractor {

    public static final int DEFAULT_TOP_N = 5;
    private static final int SNIPPET_RADIUS = 60;
    private static final int MAX_SNIPPET_LENGTH = 200;

    private static final String[] DIMENSIONS = {FieldWeights.GOAL, FieldWeights.CONTENT, FieldWeights.TECH};

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<MatchPoint> matchedPoints(ProjectAnalysisInput target, SparseVector targetVector,
                                          SparseVector peerVector, int topN) {
        return extract(target, targetVector, peerVector, topN, true);
    }

    public List<MatchPoint> diffPoints(ProjectAnalysisInput target, SparseVector targetVector,
                                       SparseVector peerVector, int topN) {
        return extract(target, targetVector, peerVector, topN, false);
    }

    private List<MatchPoint> extract(ProjectAnalysisInput target, SparseVector targetVector,
                                     SparseVector peerVector, int topN, boolean matched) {
        if (target == null || targetVector == null || targetVector.isEmpty()) {
            return Collections.emptyList();
        }
        int limit = topN <= 0 ? DEFAULT_TOP_N : topN;
        List<MatchPoint> points = new ArrayList<>();
        Set<String> usedSnippets = new LinkedHashSet<>();
        for (String dimension : DIMENSIONS) {
            String text = target.text(dimension);
            if (text == null || text.trim().isEmpty()) {
                continue;
            }
            List<String> terms = selectTerms(targetVector, peerVector, limit, matched);
            if (terms.isEmpty()) {
                continue;
            }
            String snippet = snippet(text, terms.get(0), usedSnippets);
            if (snippet.isEmpty()) {
                continue;
            }
            points.add(new MatchPoint(dimension, terms, snippet));
        }
        return points;
    }

    private List<String> selectTerms(SparseVector targetVector, SparseVector peerVector,
                                     int limit, boolean matched) {
        List<String> terms = new ArrayList<>();
        for (Map.Entry<String, Double> entry : targetVector.topTokens(Integer.MAX_VALUE)) {
            boolean shared = peerVector != null && peerVector.get(entry.getKey()) > 0D;
            if (shared == matched) {
                terms.add(entry.getKey());
                if (terms.size() >= limit) {
                    break;
                }
            }
        }
        return terms;
    }

    private String snippet(String text, String term, Set<String> used) {
        int index = text.indexOf(term);
        int start = index < 0 ? 0 : Math.max(0, index - SNIPPET_RADIUS);
        int end = index < 0
                ? Math.min(text.length(), MAX_SNIPPET_LENGTH)
                : Math.min(text.length(), index + term.length() + SNIPPET_RADIUS);
        if (end - start > MAX_SNIPPET_LENGTH) {
            end = Math.min(text.length(), start + MAX_SNIPPET_LENGTH);
        }
        String snippet = text.substring(start, end).trim();
        if (snippet.isEmpty() || !used.add(snippet)) {
            return "";
        }
        return snippet;
    }

    public String toJson(List<MatchPoint> points) {
        List<MatchPoint> safePoints = points == null ? Collections.emptyList() : points;
        try {
            return OBJECT_MAPPER.writeValueAsString(safePoints);
        } catch (JsonProcessingException exception) {
            log.warn("匹配片段序列化失败: {}", exception.getMessage());
            return "[]";
        }
    }
}

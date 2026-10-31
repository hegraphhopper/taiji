package com.taiji.fund.analysis.vector;

import com.taiji.fund.analysis.tokenizer.WeightedToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 语料级 TF-IDF 向量化。
 *
 * <p>tf = 1 + ln(加权词频)；idf = ln((N + 1) / (df + 1)) + 1；结果做 L2 归一化。
 * 必须按整批语料一次性构建，禁止每对项目单独构建。</p>
 */
public class TfIdfVectorizer {

    /**
     * 按语料批量构建向量。
     */
    public List<SparseVector> vectorize(List<List<WeightedToken>> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        int documentCount = documents.size();
        List<Map<String, Double>> termCounts = new ArrayList<>(documentCount);
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<WeightedToken> document : documents) {
            Map<String, Double> counts = countTerms(document);
            termCounts.add(counts);
            for (String token : counts.keySet()) {
                documentFrequency.merge(token, 1, Integer::sum);
            }
        }

        List<SparseVector> vectors = new ArrayList<>(documentCount);
        for (Map<String, Double> counts : termCounts) {
            Map<String, Double> rawWeights = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : counts.entrySet()) {
                double tf = tf(entry.getValue());
                double idf = idf(documentCount, documentFrequency.getOrDefault(entry.getKey(), 0));
                rawWeights.put(entry.getKey(), tf * idf);
            }
            vectors.add(SparseVector.of(rawWeights));
        }
        return vectors;
    }

    private Map<String, Double> countTerms(List<WeightedToken> document) {
        Map<String, Double> counts = new LinkedHashMap<>();
        if (document == null) {
            return counts;
        }
        for (WeightedToken token : document) {
            if (token == null || token.getToken() == null || token.getToken().isEmpty()) {
                continue;
            }
            counts.merge(token.getToken(), (double) Math.max(1, token.getWeight()), Double::sum);
        }
        return counts;
    }

    public static double tf(double weightedCount) {
        if (weightedCount <= 0D) {
            return 0D;
        }
        return 1D + Math.log(weightedCount);
    }

    public static double idf(int documentCount, int documentFrequency) {
        return Math.log((documentCount + 1D) / (documentFrequency + 1D)) + 1D;
    }

    /**
     * 语料去重后的全部 token，供解释性输出使用。
     */
    public static Set<String> vocabulary(List<List<WeightedToken>> documents) {
        Set<String> vocabulary = new HashSet<>();
        if (documents == null) {
            return vocabulary;
        }
        for (List<WeightedToken> document : documents) {
            if (document == null) {
                continue;
            }
            for (WeightedToken token : document) {
                if (token != null && token.getToken() != null && !token.getToken().isEmpty()) {
                    vocabulary.add(token.getToken());
                }
            }
        }
        return vocabulary;
    }
}

package com.taiji.fund.analysis.vector;

import com.taiji.fund.analysis.tokenizer.WeightedToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TfIdfTest {

    @Test
    @DisplayName("UT-09 相同文本余弦相似度为 1")
    void ut09_identicalDocuments() {
        List<List<WeightedToken>> corpus = Arrays.asList(
                tokens("人工智能", "神经网络", "深度学习"),
                tokens("人工智能", "神经网络", "深度学习"));
        List<SparseVector> vectors = new TfIdfVectorizer().vectorize(corpus);

        assertThat(CosineSimilarity.cosine(vectors.get(0), vectors.get(1))).isCloseTo(1D, within(1e-6));
    }

    @Test
    @DisplayName("UT-10 无共同 token 的文本余弦相似度为 0")
    void ut10_unrelatedDocuments() {
        List<List<WeightedToken>> corpus = Arrays.asList(
                tokens("人工智能", "神经网络"),
                tokens("财务报销", "发票审计"));
        List<SparseVector> vectors = new TfIdfVectorizer().vectorize(corpus);

        assertThat(CosineSimilarity.cosine(vectors.get(0), vectors.get(1))).isZero();
    }

    @Test
    @DisplayName("IDF 让语料中稀有 token 权重高于常见 token")
    void idfPrefersRareTokens() {
        List<List<WeightedToken>> corpus = Arrays.asList(
                tokens("rare", "common"),
                tokens("common"));
        List<SparseVector> vectors = new TfIdfVectorizer().vectorize(corpus);

        assertThat(vectors.get(0).get("rare")).isGreaterThan(vectors.get(0).get("common"));
    }

    @Test
    @DisplayName("术语加权提高词频权重")
    void termWeightIncreasesTf() {
        List<List<WeightedToken>> corpus = Collections.singletonList(Arrays.asList(
                new WeightedToken("term", 3), new WeightedToken("plain", 1)));
        SparseVector vector = new TfIdfVectorizer().vectorize(corpus).get(0);

        assertThat(vector.get("term")).isGreaterThan(vector.get("plain"));
    }

    @Test
    @DisplayName("空语料与空文档安全降级")
    void emptyCorpus() {
        TfIdfVectorizer vectorizer = new TfIdfVectorizer();
        assertThat(vectorizer.vectorize(Collections.emptyList())).isEmpty();
        List<SparseVector> vectors = vectorizer.vectorize(Arrays.asList(
                Collections.emptyList(), tokens("abc")));
        assertThat(vectors.get(0).isEmpty()).isTrue();
        assertThat(CosineSimilarity.cosine(vectors.get(0), vectors.get(1))).isZero();
        assertThat(JaccardSimilarity.jaccard(Collections.emptySet(), Collections.emptySet())).isZero();
    }

    private List<WeightedToken> tokens(String... values) {
        return Arrays.asList(Arrays.stream(values)
                .map(value -> new WeightedToken(value, 1))
                .toArray(WeightedToken[]::new));
    }
}

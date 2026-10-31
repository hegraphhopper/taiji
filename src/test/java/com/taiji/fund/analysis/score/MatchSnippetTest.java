package com.taiji.fund.analysis.score;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import com.taiji.fund.analysis.tokenizer.ChineseTokenizer;
import com.taiji.fund.analysis.vector.SparseVector;
import com.taiji.fund.analysis.vector.TfIdfVectorizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchSnippetTest {

    private final MatchSnippetExtractor extractor = new MatchSnippetExtractor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("UT-17 提取共享词片段与单侧差异点")
    void ut17_matchedAndDiffPoints() {
        ProjectAnalysisInput target = ProjectAnalysisInput.builder()
                .projectId(1L)
                .goalText("深度学习模型压缩算法")
                .build();
        ProjectAnalysisInput peer = ProjectAnalysisInput.builder()
                .projectId(2L)
                .goalText("深度学习模型优化")
                .build();

        List<SparseVector> vectors = new TfIdfVectorizer().vectorize(Arrays.asList(
                ChineseTokenizer.tokenize(target.getGoalText()),
                ChineseTokenizer.tokenize(peer.getGoalText())));

        List<MatchPoint> matched = extractor.matchedPoints(target, vectors.get(0), vectors.get(1), 5);
        List<MatchPoint> differences = extractor.diffPoints(target, vectors.get(0), vectors.get(1), 5);

        assertThat(matched).isNotEmpty();
        assertThat(matched.get(0).getDimension()).isEqualTo("goal");
        assertThat(matched.get(0).getTerms()).isNotEmpty();
        assertThat(matched.get(0).getSnippet()).isNotEmpty().hasSizeLessThanOrEqualTo(200);

        assertThat(differences).isNotEmpty();
        assertThat(differences.get(0).getTerms())
                .anyMatch(term -> Arrays.asList("压缩", "缩算", "算法").contains(term));
    }

    @Test
    @DisplayName("UT-17 空文本维度不产出片段")
    void ut17_emptyTextProducesNothing() {
        ProjectAnalysisInput target = ProjectAnalysisInput.builder()
                .projectId(1L)
                .goalText("")
                .build();
        ProjectAnalysisInput peer = ProjectAnalysisInput.builder()
                .projectId(2L)
                .goalText("深度学习模型优化")
                .build();
        List<SparseVector> vectors = new TfIdfVectorizer().vectorize(Arrays.asList(
                ChineseTokenizer.tokenize(target.getGoalText()),
                ChineseTokenizer.tokenize(peer.getGoalText())));

        assertThat(extractor.matchedPoints(target, vectors.get(0), vectors.get(1), 5)).isEmpty();
        assertThat(extractor.diffPoints(target, vectors.get(0), vectors.get(1), 5)).isEmpty();
    }

    @Test
    @DisplayName("UT-17 片段 JSON 结构包含 dimension/terms/snippet")
    void ut17_jsonStructure() throws Exception {
        ProjectAnalysisInput target = ProjectAnalysisInput.builder()
                .projectId(1L)
                .goalText("深度学习模型压缩算法")
                .build();
        ProjectAnalysisInput peer = ProjectAnalysisInput.builder()
                .projectId(2L)
                .goalText("深度学习模型优化")
                .build();
        List<SparseVector> vectors = new TfIdfVectorizer().vectorize(Arrays.asList(
                ChineseTokenizer.tokenize(target.getGoalText()),
                ChineseTokenizer.tokenize(peer.getGoalText())));

        String json = extractor.toJson(extractor.matchedPoints(target, vectors.get(0), vectors.get(1), 5));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.isArray()).isTrue();
        assertThat(node.get(0).path("dimension").asText()).isEqualTo("goal");
        assertThat(node.get(0).path("terms").isArray()).isTrue();
        assertThat(node.get(0).path("snippet").asText()).isNotEmpty();
    }
}

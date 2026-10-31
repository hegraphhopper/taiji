package com.taiji.fund.analysis.tokenizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ChineseTokenizerTest {

    @Test
    @DisplayName("UT-06 中文 2-gram 与英文整词分词")
    void ut06_chineseBigramsAndEnglishWords() {
        List<WeightedToken> tokens = ChineseTokenizer.tokenize("智能计算平台 support vector machine v2");
        List<String> values = tokens.stream().map(WeightedToken::getToken).collect(Collectors.toList());

        assertThat(values).contains("智能", "能计", "计算", "算平", "平台");
        assertThat(values).contains("support", "vector", "machine", "v2");
        assertThat(values).allSatisfy(token -> {
            assertThat(token).isNotEmpty();
            assertThat(token).doesNotMatch("^[\\p{P}\\p{S}]+$");
        });
        assertThat(values).doesNotContain("啊", "的");
    }

    @Test
    @DisplayName("UT-06 停用字不产生 token")
    void ut06_stopCharsRemoved() {
        List<WeightedToken> tokens = ChineseTokenizer.tokenize("基于知识图谱的构建",
                TermDictionary.of(java.util.Collections.singletonList("知识图谱")));
        List<String> values = tokens.stream().map(WeightedToken::getToken).collect(Collectors.toList());

        assertThat(values).contains("知识图谱", "构建");
        assertThat(values).filteredOn(value -> value.contains("的")).isEmpty();
        assertThat(values).doesNotContain("基于");
    }

    @Test
    @DisplayName("UT-06 纯标点与空白无 token")
    void ut06_noEmptyOrPunctuation() {
        assertThat(ChineseTokenizer.tokenize("！！！，，  。。。")).isEmpty();
        assertThat(ChineseTokenizer.tokenize(null)).isEmpty();
        assertThat(ChineseTokenizer.tokenize("   ")).isEmpty();
    }

    @Test
    @DisplayName("UT-07 术语权重为 3，普通 2-gram 权重为 1")
    void ut07_termWeight() {
        List<WeightedToken> tokens = ChineseTokenizer.tokenize("知识图谱构建",
                TermDictionary.of(java.util.Collections.singletonList("知识图谱")));

        assertThat(tokens).contains(new WeightedToken("知识图谱", ChineseTokenizer.TERM_WEIGHT));
        assertThat(tokens).contains(new WeightedToken("构建", ChineseTokenizer.NORMAL_WEIGHT));
        assertThat(tokens.stream().filter(token -> "知识图谱".equals(token.getToken())).count()).isEqualTo(1);
    }
}

package com.taiji.fund.analysis.tokenizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    @DisplayName("UT-05 null 与空白输入返回空串")
    void ut05_nullAndBlank() {
        assertThat(TextNormalizer.normalize(null)).isEmpty();
        assertThat(TextNormalizer.normalize("   \t\n ")).isEmpty();
    }

    @Test
    @DisplayName("UT-05 全角转半角且英文小写")
    void ut05_fullWidthToHalfWidthAndLowerCase() {
        assertThat(TextNormalizer.normalize("ＡＢＣ１２３　Ｘ")).isEqualTo("abc123 x");
    }

    @Test
    @DisplayName("UT-05 去 HTML 标签、控制符并统一空白")
    void ut05_htmlAndControlRemoved() {
        String normalized = TextNormalizer.normalize("<div>Hello</div>\u0000\u0007World\t\n Next");
        assertThat(normalized).isEqualTo("hello world next");
    }

    @Test
    @DisplayName("UT-05 超长文本截断到 20000 字符")
    void ut05_truncate() {
        char[] chars = new char[25000];
        Arrays.fill(chars, 'a');
        assertThat(TextNormalizer.normalize(new String(chars))).hasSize(TextNormalizer.MAX_LENGTH);
    }
}

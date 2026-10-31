package com.taiji.fund.analysis.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser(new ObjectMapper());

    @Test
    @DisplayName("UT-19 合法 JSON 解析成功")
    void ut19_validResponse() {
        LlmParseResult result = parser.parse(validJson());

        assertThat(result.isValid()).isTrue();
        assertThat(result.getResponse().getSemanticScore()).isEqualTo(88D);
        assertThat(result.getResponse().getOverlaps()).containsExactly("研究目标一致");
        assertThat(result.getResponse().getSuggestion()).isEqualTo("EXPLAIN");
    }

    @Test
    @DisplayName("UT-19 代码块包裹的 JSON 可解析")
    void ut19_codeFenceStripped() {
        LlmParseResult result = parser.parse("```json\n" + validJson() + "\n```");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("UT-19 分数越界丢弃")
    void ut19_scoreOutOfRange() {
        String json = validJson().replace("\"semanticScore\":88", "\"semanticScore\":101");
        assertThat(parser.parse(json).isValid()).isFalse();
    }

    @Test
    @DisplayName("UT-19 数组超过 10 项丢弃")
    void ut19_tooManyItems() {
        StringBuilder overlaps = new StringBuilder("[");
        for (int index = 0; index < 11; index++) {
            if (index > 0) {
                overlaps.append(',');
            }
            overlaps.append("\"重复点").append(index).append('"');
        }
        overlaps.append(']');
        String json = validJson().replace("\"overlaps\":[\"研究目标一致\"]",
                "\"overlaps\":" + overlaps);
        assertThat(parser.parse(json).isValid()).isFalse();
    }

    @Test
    @DisplayName("UT-19 单项超过 200 字丢弃")
    void ut19_itemTooLong() {
        char[] chars = new char[201];
        Arrays.fill(chars, 'a');
        String json = validJson().replace("研究目标一致", new String(chars));
        assertThat(parser.parse(json).isValid()).isFalse();
    }

    @Test
    @DisplayName("UT-19 suggestion 非枚举值丢弃")
    void ut19_invalidSuggestion() {
        String json = validJson().replace("EXPLAIN", "UNKNOWN");
        assertThat(parser.parse(json).isValid()).isFalse();
    }

    @Test
    @DisplayName("UT-19 缺字段、非 JSON、超长数组均丢弃")
    void ut19_invalidResponses() {
        assertThat(parser.parse(null).isValid()).isFalse();
        assertThat(parser.parse("not-json").isValid()).isFalse();
        assertThat(parser.parse("[]").isValid()).isFalse();
        String missingReason = "{\"semanticScore\":50,\"overlaps\":[],\"differences\":[],"
                + "\"suggestion\":\"APPROVE\"}";
        assertThat(parser.parse(missingReason).isValid()).isFalse();
        assertThat(parser.parse("{\"semanticScore\":50,\"overlaps\":[],\"differences\":[],"
                + "\"suggestion\":\"APPROVE\",\"reason\":\"ok\"}").isValid()).isTrue();
        assertThat(parser.parse("{\"semanticScore\":\"50\",\"overlaps\":[],\"differences\":[],"
                + "\"suggestion\":\"APPROVE\",\"reason\":\"ok\"}").isValid()).isFalse();
        assertThat(parser.parse("{\"semanticScore\":50,\"overlaps\":[],\"differences\":[],"
                + "\"suggestion\":\"APPROVE\",\"reason\":\"ok\",\"extra\":\"x\"}").isValid()).isTrue();
        assertThat(Collections.emptyList()).isEmpty();
    }

    private String validJson() {
        return "{\"semanticScore\":88,\"overlaps\":[\"研究目标一致\"],"
                + "\"differences\":[\"应用场景不同\"],\"suggestion\":\"EXPLAIN\","
                + "\"reason\":\"研究目标与关键技术存在重叠\"}";
    }
}

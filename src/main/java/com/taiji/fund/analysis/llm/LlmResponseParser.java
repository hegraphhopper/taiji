package com.taiji.fund.analysis.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 严格校验大模型 JSON：semanticScore 0-100、数组 ≤10 项且每项 ≤200 字、suggestion 枚举合法。
 */
@Component
public class LlmResponseParser {

    private static final int MAX_ITEMS = 10;
    private static final int MAX_ITEM_LENGTH = 200;
    private static final Set<String> SUGGESTIONS = new HashSet<>(
            Arrays.asList("MERGE", "EXPLAIN", "APPROVE", "TERMINATE"));

    private final ObjectMapper objectMapper;

    public LlmResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LlmParseResult parse(String content) {
        String json = stripCodeFence(content);
        if (json.isEmpty()) {
            return LlmParseResult.invalid("响应内容为空");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return LlmParseResult.invalid("响应不是 JSON 对象");
            }
            JsonNode scoreNode = root.get("semanticScore");
            if (scoreNode == null || !scoreNode.isNumber()) {
                return LlmParseResult.invalid("semanticScore 缺失或不是数值");
            }
            double score = scoreNode.asDouble();
            if (score < 0D || score > 100D) {
                return LlmParseResult.invalid("semanticScore 超出 0-100");
            }
            List<String> overlaps = parseStringArray(root.get("overlaps"), "overlaps");
            if (overlaps == null) {
                return LlmParseResult.invalid("overlaps 缺失、类型错误或超出长度限制");
            }
            List<String> differences = parseStringArray(root.get("differences"), "differences");
            if (differences == null) {
                return LlmParseResult.invalid("differences 缺失、类型错误或超出长度限制");
            }
            JsonNode suggestionNode = root.get("suggestion");
            if (suggestionNode == null || !suggestionNode.isTextual()) {
                return LlmParseResult.invalid("suggestion 缺失或不是字符串");
            }
            String suggestion = suggestionNode.asText().trim().toUpperCase(Locale.ROOT);
            if (!SUGGESTIONS.contains(suggestion)) {
                return LlmParseResult.invalid("suggestion 不在允许枚举内");
            }
            JsonNode reasonNode = root.get("reason");
            if (reasonNode == null || !reasonNode.isTextual()) {
                return LlmParseResult.invalid("reason 缺失或不是字符串");
            }
            return LlmParseResult.valid(new LlmResponse(score, overlaps, differences,
                    suggestion, reasonNode.asText()));
        } catch (Exception exception) {
            return LlmParseResult.invalid("JSON 解析失败: " + exception.getClass().getSimpleName());
        }
    }

    private List<String> parseStringArray(JsonNode node, String field) {
        if (node == null || !node.isArray() || node.size() > MAX_ITEMS) {
            return null;
        }
        List<String> values = new ArrayList<>(node.size());
        for (JsonNode item : node) {
            if (!item.isTextual() || item.asText().length() > MAX_ITEM_LENGTH) {
                return null;
            }
            values.add(item.asText());
        }
        return values;
    }

    private String stripCodeFence(String content) {
        if (content == null) {
            return "";
        }
        String value = content.trim();
        if (value.startsWith("```")) {
            int firstLineEnd = value.indexOf('\n');
            if (firstLineEnd >= 0) {
                value = value.substring(firstLineEnd + 1);
            }
            if (value.endsWith("```")) {
                value = value.substring(0, value.length() - 3);
            }
        }
        return value.trim();
    }
}

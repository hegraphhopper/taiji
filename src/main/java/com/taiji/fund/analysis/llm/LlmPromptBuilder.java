package com.taiji.fund.analysis.llm;

import com.taiji.fund.analysis.support.MilestoneInfo;
import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 构造大模型 Prompt。模板位于 classpath:prompts/duplicate-analysis.txt。
 */
@Slf4j
@Component
public class LlmPromptBuilder {

    private static final String TEMPLATE_RESOURCE = "/prompts/duplicate-analysis.txt";
    private static final String SYSTEM_PROMPT = "你是科研项目查重专家，只输出 JSON。";
    private static final String FALLBACK_TEMPLATE =
            "比较项目A与项目B的重复度，只输出 JSON："
                    + "{\"semanticScore\":0-100,\"overlaps\":[],\"differences\":[],"
                    + "\"suggestion\":\"MERGE|EXPLAIN|APPROVE|TERMINATE\",\"reason\":\"\"}。\n"
                    + "项目A：{{projectA}}\n项目B：{{projectB}}";
    private static final int MAX_FIELD_LENGTH = 2000;

    private final String template;

    public LlmPromptBuilder() {
        this(loadTemplate());
    }

    public LlmPromptBuilder(String template) {
        this.template = template == null || template.trim().isEmpty() ? FALLBACK_TEMPLATE : template;
    }

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildTemplate() {
        return template;
    }

    public String userPrompt(ProjectAnalysisInput first, ProjectAnalysisInput second) {
        String projectA = summary(first);
        String projectB = summary(second);
        return template.replace("{{projectA}}", projectA)
                .replace("{{projectB}}", projectB);
    }

    private String summary(ProjectAnalysisInput input) {
        if (input == null) {
            return "（缺失）";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("名称：").append(safe(input.getProjectName())).append('\n');
        builder.append("技术方向：").append(safe(input.getTechDirectionName())).append('\n');
        builder.append("项目目标：").append(limit(input.getGoalText())).append('\n');
        builder.append("研制内容：").append(limit(input.getContentText())).append('\n');
        builder.append("关键技术：").append(limit(input.getKeyTechText())).append('\n');
        builder.append("节点计划：");
        if (input.getMilestones().isEmpty()) {
            builder.append("（无）");
        } else {
            builder.append(input.getMilestones().stream()
                    .map(MilestoneInfo::getName)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .collect(Collectors.joining("、")));
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String limit(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue.length() > MAX_FIELD_LENGTH
                ? safeValue.substring(0, MAX_FIELD_LENGTH) : safeValue;
    }

    private static String loadTemplate() {
        try (InputStream stream = LlmPromptBuilder.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (stream == null) {
                log.warn("未找到大模型 Prompt 模板: {}，使用内置模板", TEMPLATE_RESOURCE);
                return FALLBACK_TEMPLATE;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException exception) {
            log.warn("加载大模型 Prompt 模板失败，使用内置模板: {}", exception.getMessage());
            return FALLBACK_TEMPLATE;
        }
    }
}

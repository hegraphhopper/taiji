package com.taiji.fund.document;

import com.taiji.fund.project.dto.SectionPreviewVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SectionParser {

    private static final Pattern HEADING = Pattern.compile(
            "^\\s*(?:第?[一二三四五六七八九十]+[、.．]|\\d+[、.．]|【)?\\s*"
                    + "(项目目标|研究目标|总体目标|研制内容|研究内容|关键技术|节点计划|进度安排|实施计划|考核指标)"
                    + "\\s*】?\\s*[:：]?\\s*(.*)$");
    private static final int MAX_ITEM_LENGTH = 4000;

    public SectionPreviewVO parse(String text) {
        SectionPreviewVO preview = new SectionPreviewVO();
        if (text == null || text.trim().isEmpty()) {
            preview.setParsed(false);
            preview.setMessage("没有可解析的正文");
            return preview;
        }

        List<String> goals = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<String> keyTechs = new ArrayList<>();
        List<String> milestones = new ArrayList<>();
        List<String> indicators = new ArrayList<>();

        Section current = null;
        boolean foundHeading = false;
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = HEADING.matcher(line);
            if (matcher.matches()) {
                current = Section.fromHeading(matcher.group(1));
                foundHeading = true;
                String rest = matcher.group(2);
                if (rest != null && !rest.trim().isEmpty()) {
                    add(current, cleanItem(rest), goals, contents, keyTechs, milestones, indicators);
                }
                continue;
            }
            if (current != null) {
                add(current, cleanItem(line), goals, contents, keyTechs, milestones, indicators);
            }
        }

        if (!foundHeading) {
            preview.setParsed(false);
            preview.setMessage("未识别到章节标题，已将全文作为研制内容候选");
            preview.setContents(limit(splitFallback(text)));
            return preview;
        }

        preview.setParsed(true);
        preview.setGoals(limit(goals));
        preview.setContents(limit(contents));
        preview.setKeyTechs(limit(keyTechs));
        preview.setMilestones(limit(milestones));
        preview.setIndicators(limit(indicators));

        if (goals.isEmpty() && contents.isEmpty()) {
            preview.setMessage("未识别到项目目标/研制内容，已提供全文候选");
            preview.setContents(limit(splitFallback(text)));
        }
        return preview;
    }

    private void add(Section section, String item,
                     List<String> goals, List<String> contents, List<String> keyTechs,
                     List<String> milestones, List<String> indicators) {
        if (item.isEmpty() || section == null) {
            return;
        }
        switch (section) {
            case GOAL:
                addIfAbsent(goals, item);
                break;
            case CONTENT:
                addIfAbsent(contents, item);
                break;
            case TECH:
                addIfAbsent(keyTechs, item);
                break;
            case MILESTONE:
                addIfAbsent(milestones, item);
                break;
            case INDICATOR:
                addIfAbsent(indicators, item);
                break;
            default:
                break;
        }
    }

    private void addIfAbsent(List<String> target, String item) {
        if (!target.contains(item)) {
            target.add(item);
        }
    }

    private String cleanItem(String value) {
        String item = value
                .replaceAll("^[-*•●]\\s*", "")
                .replaceAll("^\\d+[、.．)）]\\s*", "")
                .replaceAll("^[（(]\\d+[）)]\\s*", "")
                .trim();
        if (item.length() > MAX_ITEM_LENGTH) {
            item = item.substring(0, MAX_ITEM_LENGTH);
        }
        return item;
    }

    private List<String> splitFallback(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\n")) {
            String item = cleanItem(line);
            if (!item.isEmpty() && !result.contains(item)) {
                result.add(item);
            }
        }
        if (result.isEmpty()) {
            result.add(text.trim());
        }
        return result;
    }

    private List<String> limit(List<String> items) {
        Set<String> unique = new LinkedHashSet<>(items);
        List<String> result = new ArrayList<>(unique);
        if (result.size() > 50) {
            return new ArrayList<>(result.subList(0, 50));
        }
        return result;
    }

    private enum Section {
        GOAL,
        CONTENT,
        TECH,
        MILESTONE,
        INDICATOR;

        private static Section fromHeading(String heading) {
            switch (heading) {
                case "项目目标":
                case "研究目标":
                case "总体目标":
                    return GOAL;
                case "研制内容":
                case "研究内容":
                    return CONTENT;
                case "关键技术":
                    return TECH;
                case "节点计划":
                case "进度安排":
                case "实施计划":
                    return MILESTONE;
                case "考核指标":
                    return INDICATOR;
                default:
                    return null;
            }
        }
    }
}

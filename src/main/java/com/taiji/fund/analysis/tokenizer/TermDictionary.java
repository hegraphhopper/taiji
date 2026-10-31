package com.taiji.fund.analysis.tokenizer;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 技术术语词典（最长匹配）。纯 POJO，不依赖 Spring 容器。
 */
@Slf4j
public final class TermDictionary {

    private static final String BUNDLED_RESOURCE = "/terms/tech-terms.txt";

    private final Node root;
    private final Set<String> terms;

    private TermDictionary(Set<String> terms) {
        this.terms = Collections.unmodifiableSet(terms);
        this.root = new Node();
        for (String term : terms) {
            insert(term);
        }
    }

    public static TermDictionary empty() {
        return new TermDictionary(Collections.emptySet());
    }

    public static TermDictionary of(Collection<String> rawTerms) {
        Set<String> normalized = new LinkedHashSet<>();
        if (rawTerms != null) {
            for (String raw : rawTerms) {
                String term = normalizeTerm(raw);
                if (term != null) {
                    normalized.add(term);
                }
            }
        }
        return new TermDictionary(normalized);
    }

    /**
     * 读取内置术语文件；加载失败只记录 warn，返回空词典。
     */
    public static TermDictionary loadBundled() {
        Set<String> loaded = new LinkedHashSet<>();
        try (InputStream stream = TermDictionary.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                log.warn("未找到内置技术术语文件: {}", BUNDLED_RESOURCE);
                return empty();
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    String term = normalizeTerm(trimmed);
                    if (term != null) {
                        loaded.add(term);
                    }
                }
            }
        } catch (IOException exception) {
            log.warn("加载内置技术术语失败，将跳过术语加权: {}", exception.getMessage());
            return empty();
        }
        return new TermDictionary(loaded);
    }

    /**
     * 解析 tech_direction.keywords，兼容逗号、顿号、分号与空白分隔。
     */
    public static List<String> parseKeywords(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String part : raw.split("[,，、;；\\s]+")) {
            String term = normalizeTerm(part);
            if (term != null && !result.contains(term)) {
                result.add(term);
            }
        }
        return result;
    }

    private static String normalizeTerm(String raw) {
        if (raw == null) {
            return null;
        }
        String term = raw.trim().toLowerCase(Locale.ROOT);
        if (term.length() < 2) {
            return null;
        }
        return term;
    }

    /**
     * 从指定位置开始的最长术语匹配；未命中返回 null。
     */
    public String matchLongest(String text, int index) {
        if (text == null || index < 0 || index >= text.length()) {
            return null;
        }
        Node node = root;
        String matched = null;
        for (int cursor = index; cursor < text.length(); cursor++) {
            node = node.children.get(text.charAt(cursor));
            if (node == null) {
                break;
            }
            if (node.terminal) {
                matched = node.term;
            }
        }
        return matched;
    }

    public boolean contains(String term) {
        return term != null && terms.contains(term.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> terms() {
        return terms;
    }

    public int size() {
        return terms.size();
    }

    private void insert(String term) {
        Node node = root;
        for (int index = 0; index < term.length(); index++) {
            node = node.children.computeIfAbsent(term.charAt(index), key -> new Node());
        }
        node.terminal = true;
        node.term = term;
    }

    private static final class Node {
        private final Map<Character, Node> children = new HashMap<>();
        private boolean terminal;
        private String term;
    }
}

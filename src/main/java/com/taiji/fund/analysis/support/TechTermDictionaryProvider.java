package com.taiji.fund.analysis.support;

import com.taiji.fund.analysis.tokenizer.TermDictionary;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 合并内置术语与 tech_direction.keywords，供分词使用。
 *
 * <p>数据库读取失败时只记录 warn，保留内置词典，不阻断分析。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TechTermDictionaryProvider {

    private final TechDirectionMapper techDirectionMapper;

    private volatile TermDictionary cached;

    public TermDictionary get() {
        TermDictionary current = cached;
        if (current == null) {
            synchronized (this) {
                current = cached;
                if (current == null) {
                    current = load();
                    cached = current;
                }
            }
        }
        return current;
    }

    public synchronized TermDictionary reload() {
        cached = load();
        return cached;
    }

    private TermDictionary load() {
        Set<String> terms = new LinkedHashSet<>(TermDictionary.loadBundled().terms());
        try {
            List<TechDirection> directions = techDirectionMapper.selectList(null);
            for (TechDirection direction : directions) {
                terms.addAll(TermDictionary.parseKeywords(direction.getKeywords()));
            }
        } catch (Exception exception) {
            log.warn("读取技术方向关键词失败，仅使用内置术语词典: {}", exception.getMessage());
        }
        return TermDictionary.of(terms);
    }
}

package com.taiji.fund.analysis.tokenizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 内置停用字/停用词。设计文档列出的字与常见英文停用词。
 */
public final class StopWords {

    private static final String STOP_CHARS =
            "的了和与及或是为在对将把被从到按其这那有为以于而等一个我们项目研究技术系统进行实现基于通过";

    private static final Set<Character> CHINESE_STOP_CHARS = buildStopChars();

    private static final Set<String> ENGLISH_STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("the", "and", "for", "with", "this", "that", "from", "are", "was", "were",
                    "will", "can", "has", "have", "not", "but", "you", "your", "our", "its", "into",
                    "about", "than", "then", "they", "their", "there", "which", "when", "where")));

    private StopWords() {
    }

    private static Set<Character> buildStopChars() {
        Set<Character> chars = new HashSet<>();
        for (int index = 0; index < STOP_CHARS.length(); index++) {
            chars.add(STOP_CHARS.charAt(index));
        }
        return Collections.unmodifiableSet(chars);
    }

    public static boolean isStopChar(char value) {
        return CHINESE_STOP_CHARS.contains(value);
    }

    public static boolean isStop(String token) {
        if (token == null || token.isEmpty()) {
            return true;
        }
        if (ENGLISH_STOP_WORDS.contains(token)) {
            return true;
        }
        for (int index = 0; index < token.length(); index++) {
            if (!isStopChar(token.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}

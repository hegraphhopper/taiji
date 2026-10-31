package com.taiji.fund.analysis.tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中英混合分词：
 * <ul>
 *   <li>中文连续汉字段：术语最长匹配（权重 3），其余字符去停用字后生成 2-gram（权重 1）</li>
 *   <li>英文/数字：[a-z0-9_] 长度 ≥2 的整词</li>
 *   <li>过滤空串、纯标点与停用词</li>
 * </ul>
 */
public final class ChineseTokenizer {

    public static final int TERM_WEIGHT = 3;
    public static final int NORMAL_WEIGHT = 1;

    private static final Pattern ENGLISH_WORD = Pattern.compile("[a-z0-9_]{2,}");
    private static final Pattern PUNCTUATION_ONLY = Pattern.compile("^[\\p{P}\\p{S}]+$");
    private static final TermDictionary DEFAULT_DICTIONARY = TermDictionary.loadBundled();

    private ChineseTokenizer() {
    }

    public static List<WeightedToken> tokenize(String text) {
        return tokenize(text, DEFAULT_DICTIONARY);
    }

    public static List<WeightedToken> tokenize(String text, TermDictionary dictionary) {
        String normalized = TextNormalizer.normalize(text);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        TermDictionary effective = dictionary == null ? DEFAULT_DICTIONARY : dictionary;
        List<WeightedToken> tokens = new ArrayList<>();
        tokenizeChineseRuns(normalized, effective, tokens);
        collectEnglishWords(normalized, tokens);
        return tokens;
    }

    private static void tokenizeChineseRuns(String text, TermDictionary dictionary,
                                            List<WeightedToken> tokens) {
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            if (!isHan(codePoint)) {
                index += Character.charCount(codePoint);
                continue;
            }
            int end = index;
            while (end < text.length()) {
                int current = text.codePointAt(end);
                if (!isHan(current)) {
                    break;
                }
                end += Character.charCount(current);
            }
            tokenizeHanRun(text, index, end, dictionary, tokens);
            index = end;
        }
    }

    private static void tokenizeHanRun(String text, int start, int end, TermDictionary dictionary,
                                       List<WeightedToken> tokens) {
        StringBuilder remaining = new StringBuilder();
        int index = start;
        while (index < end) {
            String term = dictionary.matchLongest(text, index);
            if (term != null && index + term.length() <= end) {
                tokens.add(new WeightedToken(term, TERM_WEIGHT));
                index += term.length();
                continue;
            }
            char current = text.charAt(index);
            if (!StopWords.isStopChar(current)) {
                remaining.append(current);
            }
            index++;
        }
        for (int cursor = 0; cursor + 1 < remaining.length(); cursor++) {
            String gram = remaining.substring(cursor, cursor + 2);
            if (isMeaningful(gram)) {
                tokens.add(new WeightedToken(gram, NORMAL_WEIGHT));
            }
        }
    }

    private static void collectEnglishWords(String text, List<WeightedToken> tokens) {
        Matcher matcher = ENGLISH_WORD.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            if (!StopWords.isStop(word)) {
                tokens.add(new WeightedToken(word, NORMAL_WEIGHT));
            }
        }
    }

    private static boolean isMeaningful(String token) {
        return token != null
                && !token.isEmpty()
                && !PUNCTUATION_ONLY.matcher(token).matches()
                && !StopWords.isStop(token);
    }

    public static boolean isHan(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }
}

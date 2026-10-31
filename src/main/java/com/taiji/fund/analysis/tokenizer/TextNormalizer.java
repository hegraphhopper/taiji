package com.taiji.fund.analysis.tokenizer;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 文本归一化：全角转半角、去 HTML 标签、去控制符、空白统一、英文小写、截断 20000 字符。
 */
public final class TextNormalizer {

    public static final int MAX_LENGTH = 20000;

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\s]]+");

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String value = text;
        value = value.replace("\r\n", "\n").replace('\r', '\n');
        value = decodeEntities(value);
        value = HTML_TAG.matcher(value).replaceAll(" ");
        value = toHalfWidth(value);
        value = CONTROL.matcher(value).replaceAll("");
        value = value.replace('\u0000', ' ');
        value = WHITESPACE.matcher(value).replaceAll(" ");
        value = value.trim();
        value = value.toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            value = value.substring(0, MAX_LENGTH);
        }
        return value;
    }

    private static String decodeEntities(String value) {
        return value.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static String toHalfWidth(String value) {
        char[] chars = value.toCharArray();
        for (int index = 0; index < chars.length; index++) {
            char current = chars[index];
            if (current == '\u3000') {
                chars[index] = ' ';
            } else if (current >= '\uFF01' && current <= '\uFF5E') {
                chars[index] = (char) (current - 0xFEE0);
            }
        }
        return new String(chars);
    }
}

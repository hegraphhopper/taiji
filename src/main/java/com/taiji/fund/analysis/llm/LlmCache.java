package com.taiji.fund.analysis.llm;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * 大模型结果缓存：key = sha256(projectId|peerId|textHash|configVersion)，TTL 24h，容量 5000。
 *
 * <p>命中缓存不重复调用、不重复计费。</p>
 */
@Component
public class LlmCache {

    public static final long DEFAULT_TTL_MS = 24L * 60L * 60L * 1000L;
    public static final int DEFAULT_MAX_SIZE = 5000;

    private final long ttlMs;
    private final int maxSize;
    private final LongSupplier clock;
    private final Map<String, Entry> entries;

    public LlmCache() {
        this(DEFAULT_TTL_MS, DEFAULT_MAX_SIZE);
    }

    public LlmCache(long ttlMs, int maxSize) {
        this(ttlMs, maxSize, System::currentTimeMillis);
    }

    LlmCache(long ttlMs, int maxSize, LongSupplier clock) {
        this.ttlMs = ttlMs;
        this.maxSize = maxSize;
        this.clock = clock;
        this.entries = new LinkedHashMap<>(16, 0.75F, true);
    }

    public synchronized LlmResponse get(String key) {
        if (key == null) {
            return null;
        }
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (isExpired(entry)) {
            entries.remove(key);
            return null;
        }
        return entry.response;
    }

    public synchronized void put(String key, LlmResponse response) {
        if (key == null || response == null) {
            return;
        }
        entries.put(key, new Entry(response, clock.getAsLong()));
        evictExpired();
        while (entries.size() > maxSize) {
            Iterator<String> iterator = entries.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    public String key(Long projectId, Long peerId, String textHash, int configVersion) {
        return hash(String.valueOf(projectId), String.valueOf(peerId),
                textHash == null ? "" : textHash, String.valueOf(configVersion));
    }

    public static String hash(String... parts) {
        StringBuilder builder = new StringBuilder();
        if (parts != null) {
            for (int index = 0; index < parts.length; index++) {
                if (index > 0) {
                    builder.append('|');
                }
                builder.append(parts[index] == null ? "" : parts[index]);
            }
        }
        return sha256(builder.toString());
    }

    public static String hashText(String... texts) {
        return hash(texts);
    }

    private boolean isExpired(Entry entry) {
        return clock.getAsLong() - entry.createdAt > ttlMs;
    }

    private void evictExpired() {
        entries.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b & 0xFF));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }

    private static final class Entry {
        private final LlmResponse response;
        private final long createdAt;

        private Entry(LlmResponse response, long createdAt) {
            this.response = response;
            this.createdAt = createdAt;
        }
    }
}

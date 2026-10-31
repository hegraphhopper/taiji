package com.taiji.fund.analysis.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LlmCacheTest {

    @Test
    @DisplayName("UT-21 key 由项目、对端、文本 hash 与配置版本共同决定")
    void ut21_keyDependsOnAllParts() {
        LlmCache cache = new LlmCache();
        String base = cache.key(1L, 2L, "hash", 3);

        assertThat(base).isEqualTo(cache.key(1L, 2L, "hash", 3));
        assertThat(base).isNotEqualTo(cache.key(1L, 2L, "hash", 4));
        assertThat(base).isNotEqualTo(cache.key(1L, 3L, "hash", 3));
        assertThat(base).isNotEqualTo(cache.key(1L, 2L, "other", 3));
        assertThat(LlmCache.hashText("a", "b")).isEqualTo(LlmCache.hashText("a", "b"));
    }

    @Test
    @DisplayName("UT-21 相同 key 命中，不同版本未命中")
    void ut21_cacheHit() {
        LlmCache cache = new LlmCache();
        LlmResponse response = new LlmResponse(90D, null, null, "MERGE", "reason");
        cache.put(cache.key(1L, 2L, "h", 1), response);

        assertThat(cache.get(cache.key(1L, 2L, "h", 1))).isSameAs(response);
        assertThat(cache.get(cache.key(1L, 2L, "h", 2))).isNull();
    }

    @Test
    @DisplayName("UT-21 TTL 过期后淘汰")
    void ut21_ttlExpiry() {
        AtomicLong clock = new AtomicLong(1000L);
        LlmCache cache = new LlmCache(100L, 10, clock::get);
        LlmResponse response = new LlmResponse(80D, null, null, "EXPLAIN", "r");
        cache.put("k", response);

        clock.set(1100L);
        assertThat(cache.get("k")).isSameAs(response);
        clock.set(1101L);
        assertThat(cache.get("k")).isNull();
    }

    @Test
    @DisplayName("UT-21 容量超限按最旧淘汰")
    void ut21_capacityEviction() {
        LlmCache cache = new LlmCache(60000L, 2);
        LlmResponse first = new LlmResponse(1D, null, null, "APPROVE", "a");
        LlmResponse second = new LlmResponse(2D, null, null, "APPROVE", "b");
        LlmResponse third = new LlmResponse(3D, null, null, "APPROVE", "c");
        cache.put("k1", first);
        cache.put("k2", second);
        assertThat(cache.get("k1")).isSameAs(first);
        cache.put("k3", third);

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get("k2")).isNull();
        assertThat(cache.get("k1")).isSameAs(first);
        assertThat(cache.get("k3")).isSameAs(third);
    }
}

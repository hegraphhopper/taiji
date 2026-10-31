package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AttributionTest {

    private final AttributionResolver resolver = new AttributionResolver();

    @Test
    @DisplayName("UT-14 同组归因为 SELF_GROUP")
    void ut14_sameGroup() {
        Attribution attribution = resolver.resolve(
                project(1L, 10L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                project(2L, 10L, LocalDateTime.of(2026, 5, 1, 0, 0)), 30);

        assertThat(attribution.getRelation()).isEqualTo(Attribution.RELATION_SELF_GROUP);
        assertThat(attribution.getDirection()).isEqualTo(Attribution.DIRECTION_SELF_GROUP);
    }

    @Test
    @DisplayName("UT-15 跨组时间差 31 天区分在先方")
    void ut15_crossGroupEarlier() {
        Attribution attribution = resolver.resolve(
                project(1L, 10L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                project(2L, 20L, LocalDateTime.of(2026, 2, 1, 0, 0)), 30);

        assertThat(attribution.getRelation()).isEqualTo(Attribution.RELATION_CROSS_GROUP);
        assertThat(attribution.getDirection()).isEqualTo(Attribution.DIRECTION_A_EARLIER);
        assertThat(attribution.getEarlierProjectId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("UT-15 跨组时间差 30/29 天归为同期")
    void ut15_simultaneousBoundary() {
        Attribution thirty = resolver.resolve(
                project(1L, 10L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                project(2L, 20L, LocalDateTime.of(2026, 1, 31, 0, 0)), 30);
        Attribution twentyNine = resolver.resolve(
                project(3L, 10L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                project(4L, 20L, LocalDateTime.of(2026, 1, 30, 0, 0)), 30);

        assertThat(thirty.getDirection()).isEqualTo(Attribution.DIRECTION_SIMULTANEOUS);
        assertThat(thirty.getEarlierProjectId()).isEqualTo(1L);
        assertThat(twentyNine.getDirection()).isEqualTo(Attribution.DIRECTION_SIMULTANEOUS);
    }

    @Test
    @DisplayName("UT-15 提交时间完全相等按 projectId 小者为先")
    void ut15_equalTimeUsesProjectId() {
        Attribution attribution = resolver.resolve(
                project(2L, 10L, LocalDateTime.of(2026, 1, 1, 0, 0)),
                project(1L, 20L, LocalDateTime.of(2026, 1, 1, 0, 0)), 30);

        assertThat(attribution.getDirection()).isEqualTo(Attribution.DIRECTION_B_EARLIER);
        assertThat(attribution.getEarlierProjectId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("UT-16 submit_time 缺失时回退 created_at")
    void ut16_fallbackToCreatedAt() {
        ProjectAnalysisInput first = ProjectAnalysisInput.builder()
                .projectId(1L).groupId(10L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        ProjectAnalysisInput second = project(2L, 20L, LocalDateTime.of(2026, 2, 15, 0, 0));

        Attribution attribution = resolver.resolve(first, second, 30);
        assertThat(attribution.getEarlierProjectId()).isEqualTo(1L);
        assertThat(attribution.getDirection()).isEqualTo(Attribution.DIRECTION_A_EARLIER);
    }

    @Test
    @DisplayName("UT-15 较晚方作为第一参数时方向为 B_EARLIER")
    void ut15_laterFirstParameter() {
        Attribution attribution = resolver.resolve(
                project(2L, 20L, LocalDateTime.of(2026, 3, 1, 0, 0)),
                project(1L, 10L, LocalDateTime.of(2026, 1, 1, 0, 0)), 30);

        assertThat(attribution.getDirection()).isEqualTo(Attribution.DIRECTION_B_EARLIER);
        assertThat(attribution.getEarlierProjectId()).isEqualTo(1L);
    }

    private ProjectAnalysisInput project(Long id, Long groupId, LocalDateTime submitTime) {
        return ProjectAnalysisInput.builder()
                .projectId(id)
                .groupId(groupId)
                .submitTime(submitTime)
                .createdAt(submitTime)
                .build();
    }
}

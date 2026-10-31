package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.Direction;
import com.taiji.fund.analysis.result.Relation;
import com.taiji.fund.analysis.score.DuplicateLevel;
import com.taiji.fund.project.domain.Project;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AnalysisRunIntegrationTest extends AbstractAnalysisTest {

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("UT-22 全量运行落库、快照聚合与项目分回写一致")
    void ut22FullRunPersistsAndAggregates() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        Long groupB = groupId("GRP-DATA");
        LocalDateTime submitTime = LocalDateTime.of(2026, 1, 10, 9, 0);
        long projectA = buildProject(groupA, "同组项目A", submitTime);
        long projectB = buildProject(groupA, "同组项目B", submitTime);
        long projectC = buildProject(groupB, "跨组项目C", submitTime.plusDays(10));

        JsonNode body = triggerRunData(pmoToken(), "{\"triggerType\":\"FULL\"}");
        assertThat(body.path("code").asInt()).isZero();
        JsonNode data = body.path("data");
        assertThat(data.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(data.path("projectCount").asInt()).isEqualTo(3);
        assertThat(data.path("pairCount").asInt()).isEqualTo(3);
        assertThat(data.path("llmUsed").asBoolean()).isFalse();
        assertThat(data.path("durationMs").asLong()).isGreaterThan(0L);

        long runId = data.path("id").asLong();
        List<AnalysisPair> pairs = pairsOfRun(runId);
        assertThat(pairs).hasSize(3);
        assertThat(pairs).allSatisfy(pair -> {
            assertThat(pair.getProjectId()).isLessThan(pair.getPeerProjectId());
            assertThat(pair.getTotalScore()).isEqualByComparingTo("100.00");
            assertThat(pair.getLevel()).isEqualTo(DuplicateLevel.HIGH.name());
            assertThat(pair.getMatchedPoints()).isNotBlank();
            assertThat(pair.getLlmScore()).isNull();
        });

        List<DuplicateSnapshot> snapshots = snapshotsOfRun(runId);
        assertThat(snapshots).hasSize(3);
        assertThat(snapshots).extracting(DuplicateSnapshot::getProjectId)
                .containsExactlyInAnyOrder(projectA, projectB, projectC);
        for (DuplicateSnapshot snapshot : snapshots) {
            assertAggregates(snapshot, pairs);
        }

        DuplicateSnapshot snapshotA = snapshots.stream()
                .filter(snapshot -> snapshot.getProjectId().equals(projectA))
                .findFirst().orElseThrow(IllegalStateException::new);
        assertThat(snapshotA.getMaxScore()).isEqualByComparingTo("100.00");
        assertThat(snapshotA.getAvgTopScore()).isEqualByComparingTo("100.00");
        assertThat(snapshotA.getHighPairCount()).isEqualTo(2);
        assertThat(snapshotA.getSelfGroupCount()).isEqualTo(1);
        assertThat(snapshotA.getCrossGroupCount()).isEqualTo(1);
        assertThat(snapshotA.getSimultaneousCount()).isEqualTo(1);
        assertThat(snapshotA.getRiskExposure()).isEqualByComparingTo("1000000.00");

        Project updatedA = projectMapper.selectById(projectA);
        assertThat(updatedA.getDuplicateScore()).isEqualByComparingTo("100.00");
        assertThat(updatedA.getRiskLevel()).isEqualTo(DuplicateLevel.HIGH.name());
        Project updatedC = projectMapper.selectById(projectC);
        assertThat(updatedC.getDuplicateScore()).isEqualByComparingTo("100.00");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("UT-22 单项目运行只分析目标项目且只更新目标快照")
    void ut22ProjectRunScopesToTarget() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        LocalDateTime submitTime = LocalDateTime.of(2026, 2, 1, 9, 0);
        long target = buildProject(groupA, "目标项目", submitTime);
        long peer = buildProject(groupA, "对照项目", submitTime);

        JsonNode data = triggerRunData(pmoToken(),
                "{\"triggerType\":\"PROJECT\",\"projectId\":" + target + "}").path("data");
        assertThat(data.path("status").asText()).isEqualTo("SUCCESS");
        long runId = data.path("id").asLong();
        List<DuplicateSnapshot> snapshots = snapshotsOfRun(runId);
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getProjectId()).isEqualTo(target);
        assertThat(pairsOfRun(runId)).allSatisfy(pair ->
                assertThat(pair.getProjectId().equals(target)
                        || pair.getPeerProjectId().equals(target)).isTrue());
        assertThat(projectMapper.selectById(peer).getDuplicateScore()).isNull();
    }

    private void assertAggregates(DuplicateSnapshot snapshot, List<AnalysisPair> pairs) {
        List<AnalysisPair> involving = pairs.stream()
                .filter(pair -> snapshot.getProjectId().equals(pair.getProjectId())
                        || snapshot.getProjectId().equals(pair.getPeerProjectId()))
                .collect(Collectors.toList());
        double max = involving.stream()
                .mapToDouble(pair -> pair.getTotalScore().doubleValue())
                .max().orElse(0D);
        assertThat(snapshot.getMaxScore()).isEqualByComparingTo(
                BigDecimal.valueOf(max).setScale(2, BigDecimal.ROUND_HALF_UP));

        int high = (int) involving.stream()
                .filter(pair -> DuplicateLevel.HIGH.name().equals(pair.getLevel())).count();
        int self = (int) involving.stream()
                .filter(pair -> Relation.SELF_GROUP.name().equals(pair.getRelation())).count();
        int cross = (int) involving.stream()
                .filter(pair -> Relation.CROSS_GROUP.name().equals(pair.getRelation())).count();
        int simultaneous = (int) involving.stream()
                .filter(pair -> Direction.SIMULTANEOUS.name().equals(pair.getDirection())).count();
        int ourEarlier = (int) involving.stream()
                .filter(pair -> snapshot.getProjectId().equals(pair.getEarlierProjectId())
                        && !Direction.SIMULTANEOUS.name().equals(pair.getDirection())
                        && !Direction.SELF_GROUP.name().equals(pair.getDirection()))
                .count();
        int peerEarlier = (int) involving.stream()
                .filter(pair -> !snapshot.getProjectId().equals(pair.getEarlierProjectId())
                        && !Direction.SIMULTANEOUS.name().equals(pair.getDirection())
                        && !Direction.SELF_GROUP.name().equals(pair.getDirection()))
                .count();

        assertThat(snapshot.getHighPairCount()).isEqualTo(high);
        assertThat(snapshot.getSelfGroupCount()).isEqualTo(self);
        assertThat(snapshot.getCrossGroupCount()).isEqualTo(cross);
        assertThat(snapshot.getSimultaneousCount()).isEqualTo(simultaneous);
        assertThat(snapshot.getOurEarlierCount()).isEqualTo(ourEarlier);
        assertThat(snapshot.getPeerEarlierCount()).isEqualTo(peerEarlier);
    }
}

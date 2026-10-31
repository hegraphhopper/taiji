package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.RunStatus;
import com.taiji.fund.analysis.run.AnalysisBatch;
import com.taiji.fund.analysis.run.AnalysisResultWriter;
import com.taiji.fund.project.domain.Project;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AnalysisFailureTest extends AbstractAnalysisTest {

    @SpyBean
    private AnalysisResultWriter resultWriter;

    @Test
    @DisplayName("UT-24 落库阶段失败：run=FAILED、明细为空、项目分未被污染")
    void ut24FailureCleansUp() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        long projectA = buildProject(groupA, "失败项目A", LocalDateTime.of(2026, 4, 1, 9, 0));
        buildProject(groupA, "失败项目B", LocalDateTime.of(2026, 4, 1, 9, 0));

        doThrow(new IllegalStateException("simulated persist failure"))
                .when(resultWriter).persist(any(AnalysisBatch.class));

        MvcResult result = triggerRun(pmoToken(), "{\"triggerType\":\"FULL\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        JsonNode body = readJson(result);
        assertThat(body.path("code").asInt()).isEqualTo(50000);

        AnalysisRun run = latestRun();
        assertThat(run).isNotNull();
        assertThat(run.getStatus()).isEqualTo(RunStatus.FAILED.name());
        assertThat(run.getMessage()).isNotBlank();

        Long pairs = analysisPairMapper.selectCount(Wrappers.<AnalysisPair>lambdaQuery()
                .eq(AnalysisPair::getRunId, run.getId()));
        Long snapshots = snapshotMapper.selectCount(Wrappers.<DuplicateSnapshot>lambdaQuery()
                .eq(DuplicateSnapshot::getRunId, run.getId()));
        assertThat(pairs).isZero();
        assertThat(snapshots).isZero();

        Project project = projectMapper.selectById(projectA);
        assertThat(project.getDuplicateScore()).isNull();
        assertThat(project.getRiskLevel()).isNull();
    }
}

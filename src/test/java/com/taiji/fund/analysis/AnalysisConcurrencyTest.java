package com.taiji.fund.analysis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.RunStatus;
import com.taiji.fund.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS,
        methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class AnalysisConcurrencyTest extends AbstractAnalysisTest {

    @Test
    @DisplayName("UT-23 已有运行进行中时再次触发返回 409，不产生第二套明细")
    void ut23RejectsWhenAnotherRunIsRunning() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        buildProject(groupA, "并发项目A", LocalDateTime.of(2026, 3, 1, 9, 0));
        buildProject(groupA, "并发项目B", LocalDateTime.of(2026, 3, 1, 9, 0));

        AnalysisRun running = insertRun("FULL", RunStatus.RUNNING.name(), null);
        MvcResult result = triggerRun(pmoToken(), "{\"triggerType\":\"FULL\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = readJson(result);
        assertThat(body.path("code").asInt()).isEqualTo(ErrorCode.ANALYSIS_RUN_CONFLICT.getCode());

        Long runCount = analysisRunMapper.selectCount(Wrappers.<AnalysisRun>lambdaQuery());
        assertThat(runCount).isEqualTo(1L);
        assertThat(analysisPairMapper.selectCount(
                Wrappers.<com.taiji.fund.analysis.result.AnalysisPair>lambdaQuery())).isZero();
        assertThat(snapshotMapper.selectCount(
                Wrappers.<com.taiji.fund.analysis.result.DuplicateSnapshot>lambdaQuery())).isZero();

        analysisRunMapper.deleteById(running.getId());
        MvcResult retried = triggerRun(pmoToken(), "{\"triggerType\":\"FULL\"}");
        assertThat(retried.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(retried).path("data").path("status").asText()).isEqualTo("SUCCESS");
    }
}

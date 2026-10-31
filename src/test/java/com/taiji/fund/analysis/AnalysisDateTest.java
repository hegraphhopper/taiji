package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.AnalysisRunMapper;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AnalysisDateTest extends AbstractAnalysisTest {

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("UT-26 ADMIN 指定 analysisDate：语料按提交时间过滤，快照日期使用指定日期")
    void ut26AnalysisDateFiltersCorpus() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        long early = buildProject(groupA, "历史项目", LocalDateTime.of(2020, 6, 1, 9, 0));
        long late = buildProject(groupA, "未来项目", LocalDateTime.of(2030, 6, 1, 9, 0));

        MvcResult result = triggerRun(adminToken(),
                "{\"triggerType\":\"FULL\",\"analysisDate\":\"2025-01-01\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode data = readJson(result).path("data");
        assertThat(data.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(data.path("projectCount").asInt()).isEqualTo(1);

        long runId = data.path("id").asLong();
        List<DuplicateSnapshot> snapshots = snapshotsOfRun(runId);
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getProjectId()).isEqualTo(early);
        assertThat(snapshots.get(0).getAnalysisDate()).isEqualTo(LocalDate.of(2025, 1, 1));

        AnalysisRun run = analysisRunMapper.selectById(runId);
        assertThat(run.getAnalysisDate()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(projectMapper.selectById(late).getDuplicateScore()).isNull();
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("UT-26 非 ADMIN 指定 analysisDate 返回 403")
    void ut26NonAdminCannotBackfill() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        buildProject(groupA, "回填权限项目", LocalDateTime.of(2020, 6, 1, 9, 0));

        MvcResult result = triggerRun(pmoToken(),
                "{\"triggerType\":\"FULL\",\"analysisDate\":\"2025-01-01\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(readJson(result).path("code").asInt()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
        assertThat(analysisRunMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<AnalysisRun>lambdaQuery())).isZero();
    }
}

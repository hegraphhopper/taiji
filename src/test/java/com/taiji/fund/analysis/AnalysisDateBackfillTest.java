package com.taiji.fund.analysis;

import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.project.domain.Project;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A4 回填依赖：analysisDate 运行只写快照，不回写项目当前重复分/风险等级。
 */
class AnalysisDateBackfillTest extends AbstractAnalysisTest {

    @Test
    void backfillDoesNotOverwriteProjectScore() throws Exception {
        Long projectId = buildProject(groupId("GRP-INTELLIGENCE"), "回填不改分项目",
                LocalDateTime.now().minusDays(1));
        Project preset = new Project();
        preset.setId(projectId);
        preset.setDuplicateScore(new BigDecimal("77.77"));
        preset.setRiskLevel("HIGH");
        projectMapper.updateById(preset);

        String token = adminToken();
        String body = "{\"triggerType\":\"FULL\",\"analysisDate\":\"" + LocalDate.now() + "\"}";
        org.springframework.test.web.servlet.MvcResult result = triggerRun(token, body);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(result).path("data").path("status").asText()).isEqualTo("SUCCESS");

        Project after = projectMapper.selectById(projectId);
        assertThat(after.getDuplicateScore()).isEqualByComparingTo("77.77");
        assertThat(after.getRiskLevel()).isEqualTo("HIGH");
        assertThat(after.getStatus()).isEqualTo(ProjectStatus.SUBMITTED.getCode());
    }
}

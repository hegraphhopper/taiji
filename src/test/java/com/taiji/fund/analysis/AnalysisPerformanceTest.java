package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.enums.ProjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UT-29 性能门禁：300 项目全量本地分析 ≤5s。默认被 surefire 排除，手动执行：
 * {@code mvn test -Dgroups=perf -DexcludedGroups=}。
 */
@Tag("perf")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AnalysisPerformanceTest extends AbstractAnalysisTest {

    private static final int PROJECT_COUNT = 300;

    @Test
    @DisplayName("UT-29 300 项目全量分析耗时 ≤5s")
    void ut29FullRunWithinBudget() throws Exception {
        Long groupA = groupId("GRP-INTELLIGENCE");
        Long groupB = groupId("GRP-DATA");
        LocalDateTime submitTime = LocalDateTime.of(2026, 9, 1, 9, 0);

        for (int index = 0; index < PROJECT_COUNT; index++) {
            Long groupId = index % 3 == 0 ? groupB : groupA;
            buildProject(groupId, "性能项目-" + index,
                    buildText("研究目标", index),
                    buildText("研制内容", index),
                    "多模态特征融合 知识图谱推理 边缘计算 安全审计",
                    "原型开发", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    new BigDecimal("1000000.00"), submitTime, ProjectStatus.SUBMITTED.getCode());
        }

        String token = pmoToken();
        long start = System.currentTimeMillis();
        JsonNode body = triggerRunData(token, "{\"triggerType\":\"FULL\"}");
        long elapsed = System.currentTimeMillis() - start;

        assertThat(body.path("code").asInt()).isZero();
        JsonNode data = body.path("data");
        assertThat(data.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(data.path("projectCount").asInt()).isEqualTo(PROJECT_COUNT);
        assertThat(data.path("pairCount").asInt()).isLessThanOrEqualTo(PROJECT_COUNT * 5);

        System.out.println("[perf] 300 项目 FULL 运行耗时 " + elapsed + " ms，"
                + "pairCount=" + data.path("pairCount").asInt()
                + "，durationMs=" + data.path("durationMs").asLong());
        assertThat(elapsed).isLessThan(5000L);
    }

    private String buildText(String dimension, int index) {
        StringBuilder builder = new StringBuilder();
        builder.append("面向工业互联网场景的智能分析平台").append(dimension)
                .append("，构建数据采集、特征提取、模型训练与推理服务，支持实时监测与异常预警。");
        for (int repeat = 0; repeat < 6; repeat++) {
            builder.append("围绕多模态数据融合、知识图谱推理、边缘侧低时延计算与安全审计能力开展研究。");
        }
        builder.append("本项目的差异化主题为第").append(index % 17)
                .append("类技术路线与第").append(index).append("号场景。");
        return builder.toString();
    }
}

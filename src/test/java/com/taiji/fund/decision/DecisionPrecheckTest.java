package com.taiji.fund.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectReview;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-D03/D04/D05/D08/D09：PROJECT/TEXT 预检、专家建议、权限与降级。
 */
class DecisionPrecheckTest extends AbstractDecisionTest {

    @Test
    @DisplayName("UT-D03/D09 高重复项目预检返回规则结论、金额与本地降级标记")
    void ut03ProjectPrecheckHighDuplicate() throws Exception {
        Long group = groupId("GRP-INTELLIGENCE");
        String text = uniqueText("高重复预检");
        long peer = submittedProject(group, uniqueProjectName("高重复对照"), text);
        long draft = draftProject(group, uniqueProjectName("高重复草稿"), text);
        String token = tokenOf(createGroupUser("decision-precheck", group));

        JsonNode data = precheck(token, "{\"mode\":\"PROJECT\",\"projectId\":" + draft + "}");

        BigDecimal score = data.path("riskScore").decimalValue();
        assertThat(score).isGreaterThanOrEqualTo(new BigDecimal("90.00"));
        assertThat(data.path("level").asText()).isEqualTo("HIGH");
        assertThat(data.path("recommendation").asText()).isIn("MERGE", "TERMINATE");
        assertThat(data.path("topPeers").size()).isGreaterThan(0);
        assertThat(data.path("topPeers").get(0).path("peer").path("id").asLong()).isEqualTo(peer);
        assertThat(data.path("topPeers").get(0).path("matchedPoints").size()).isGreaterThan(0);
        assertThat(data.path("llmUsed").asBoolean()).isFalse();
        assertThat(data.path("reason").asText()).startsWith("本地算法结果");

        BigDecimal expectedSavings = new BigDecimal("1000000.00").multiply(score)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        assertThat(data.path("savingsEstimate").decimalValue())
                .isEqualByComparingTo(expectedSavings);

        Project after = projectMapper.selectById(draft);
        assertThat(after.getDuplicateScore()).isNull();
        assertThat(after.getStatus()).isEqualTo(ProjectStatus.DRAFT.getCode());
    }

    @Test
    @DisplayName("UT-D04 TEXT 预检不落项目，生成摘要且金额可选")
    void ut04TextPrecheckHashAndOptionalBudget() throws Exception {
        Long group = groupId("GRP-INTELLIGENCE");
        String text = uniqueText("临时文本预检");
        submittedProject(group, uniqueProjectName("文本对照"), text);
        String token = tokenOf(createGroupUser("decision-text", group));

        JsonNode withoutBudget = precheck(token, textBody(text, null));
        assertThat(withoutBudget.path("projectId").isNull()).isTrue();
        assertThat(withoutBudget.path("inputHash").asText()).hasSize(64);
        assertThat(withoutBudget.path("savingsEstimate").isNull()).isTrue();
        assertThat(withoutBudget.path("topPeers").size()).isGreaterThan(0);

        JsonNode withBudget = precheck(token, textBody(text, "500000.00"));
        BigDecimal score = withBudget.path("riskScore").decimalValue();
        BigDecimal expected = new BigDecimal("500000.00").multiply(score)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        assertThat(withBudget.path("savingsEstimate").decimalValue())
                .isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("UT-D05 建议专家同方向优先")
    void ut05SuggestedExpertsPreferSameDirection() throws Exception {
        Long group = groupId("GRP-INTELLIGENCE");
        String text = uniqueText("专家建议");
        submittedProject(group, uniqueProjectName("专家对照"), text);
        long draft = draftProject(group, uniqueProjectName("专家目标"), text);

        User busy = createUser("decision-expert-a", PASSWORD, UserStatus.ENABLED,
                group, RoleCode.EXPERT, 0);
        User idle = createUser("decision-expert-b", PASSWORD, UserStatus.ENABLED,
                group, RoleCode.EXPERT, 0);
        long reviewed = buildProject(group, uniqueProjectName("同方向评审项目"),
                uniqueText("评审项目"), uniqueText("评审项目"), uniqueText("评审项目"),
                "原型开发", LocalDateHolder.START, LocalDateHolder.END,
                new BigDecimal("1000000.00"), LocalDateTime.now().minusDays(5),
                ProjectStatus.REVIEWING.getCode());
        review(reviewed, busy.getId());
        review(reviewed, busy.getId());

        String token = tokenOf(createGroupUser("decision-expert", group));
        JsonNode data = precheck(token, "{\"mode\":\"PROJECT\",\"projectId\":" + draft + "}");

        JsonNode experts = data.path("suggestedExperts");
        assertThat(experts.size()).isGreaterThan(0).isLessThanOrEqualTo(3);
        int busyIndex = indexOf(experts, busy.getId());
        int idleIndex = indexOf(experts, idle.getId());
        assertThat(busyIndex).isGreaterThanOrEqualTo(0);
        if (idleIndex >= 0) {
            assertThat(busyIndex).isLessThan(idleIndex);
        }
        assertThat(experts.get(busyIndex).path("sameDirectionReviews").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(experts.toString()).doesNotContain("email").doesNotContain("phone");
    }

    @Test
    @DisplayName("UT-D08 组用户不可预检他组，专家不可保存，未登录 401")
    void ut08Permissions() throws Exception {
        Long ownGroup = groupId("GRP-INTELLIGENCE");
        Long otherGroup = groupId("GRP-DATA");
        String text = uniqueText("权限预检");
        submittedProject(ownGroup, uniqueProjectName("权限对照"), text);
        long otherProject = submittedProject(otherGroup, uniqueProjectName("他组项目"),
                uniqueText("他组项目"));
        String groupToken = tokenOf(createGroupUser("decision-scope", ownGroup));

        mockMvc.perform(post("/api/v1/decision/precheck")
                        .header("Authorization", bearer(groupToken))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"PROJECT\",\"projectId\":" + otherProject + "}"))
                .andExpect(status().isForbidden());

        User expert = createUser("decision-scope-expert", PASSWORD, UserStatus.ENABLED,
                ownGroup, RoleCode.EXPERT, 0);
        String expertToken = tokenOf(expert);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("riskScore", new BigDecimal("80.00"));
        record.put("level", "HIGH");
        record.put("recommendation", "MERGE");
        record.put("similarProjects", "[]");
        mockMvc.perform(post("/api/v1/decision/records")
                        .header("Authorization", bearer(expertToken))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/decision/precheck")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEXT\",\"goalText\":\"未登录\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void review(long projectId, Long reviewerId) {
        ProjectReview review = new ProjectReview();
        review.setProjectId(projectId);
        review.setReviewerId(reviewerId);
        review.setRound(1);
        review.setScore(new BigDecimal("80.00"));
        review.setOpinion("自动化测试评审");
        review.setResult("PASS");
        review.setCreatedAt(LocalDateTime.now());
        projectReviewMapper.insert(review);
    }

    private int indexOf(JsonNode experts, Long userId) {
        for (int index = 0; index < experts.size(); index++) {
            if (experts.get(index).path("id").asLong() == userId) {
                return index;
            }
        }
        return -1;
    }

    private String textBody(String text, String budget) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", "TEXT");
        body.put("goalText", text);
        body.put("contentText", text);
        body.put("techText", text + " 边缘计算 知识图谱");
        if (budget != null) {
            body.put("budget", new BigDecimal(budget));
        }
        return objectMapper.writeValueAsString(body);
    }
}

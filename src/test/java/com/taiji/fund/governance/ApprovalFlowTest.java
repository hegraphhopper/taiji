package com.taiji.fund.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 审批流：形式审查、专家 PASS 门禁、立项、执行、结项、驳回重提、终止。
 */
class ApprovalFlowTest extends AbstractGovernanceTest {

    @Test
    @DisplayName("UT-G06 PASS=2 门禁与完整立项执行结项链路")
    void ut06PassGateAndFullFlow() throws Exception {
        String admin = adminToken();
        putOk("/api/v1/system/configs/review.requiredPassCount", admin, "{\"value\":\"2\"}");
        User owner = createGroupUser("gov-flow", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("gov-flow-m", owner.getGroupId());
        String ownerToken = tokenOf(owner);
        String pmo = pmoToken();
        User expertA = createExpert("gov-flow-ea");
        User expertB = createExpert("gov-flow-eb");

        long projectId = createSubmittedProject(ownerToken, owner.getGroupId(), owner.getId(),
                member.getId(), uniqueProjectName("gov-flow"));
        acceptProject(pmo, projectId);
        review(tokenOf(expertA), projectId, "PASS", "方案可行");
        JsonNode blocked = perform(post("/api/v1/projects/{id}/actions", projectId), pmo, 400,
                "{\"action\":\"APPROVE\",\"comment\":\"同意\"}");
        assertThat(blocked.path("message").asText()).contains("至少 2");
        assertStatus(ownerToken, projectId, "REVIEWING");

        review(tokenOf(expertB), projectId, "PASS", "同意立项");
        JsonNode approved = postOk("/api/v1/projects/" + projectId + "/actions", pmo,
                "{\"action\":\"APPROVE\",\"comment\":\"同意立项\"}").path("data");
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("reviewPassCount").asInt()).isEqualTo(2);
        assertThat(approved.path("requiredPassCount").asInt()).isEqualTo(2);

        JsonNode started = postOk("/api/v1/projects/" + projectId + "/actions", ownerToken,
                "{\"action\":\"START\"}").path("data");
        assertThat(started.path("status").asText()).isEqualTo("IN_PROGRESS");
        JsonNode completed = postOk("/api/v1/projects/" + projectId + "/actions", ownerToken,
                "{\"action\":\"COMPLETE\"}").path("data");
        assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("UT-G07 驳回后重提进入新一轮，旧 PASS 按专家去重仍计入")
    void ut07RejectAndNewRound() throws Exception {
        String admin = adminToken();
        putOk("/api/v1/system/configs/review.requiredPassCount", admin, "{\"value\":\"2\"}");
        User owner = createGroupUser("gov-round", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("gov-round-m", owner.getGroupId());
        String ownerToken = tokenOf(owner);
        String pmo = pmoToken();
        User expertA = createExpert("gov-round-ea");
        User expertB = createExpert("gov-round-eb");

        long projectId = createSubmittedProject(ownerToken, owner.getGroupId(), owner.getId(),
                member.getId(), uniqueProjectName("gov-round"));
        acceptProject(pmo, projectId);
        review(tokenOf(expertA), projectId, "PASS", "首轮通过");

        JsonNode rejected = postOk("/api/v1/projects/" + projectId + "/actions", pmo,
                "{\"action\":\"REJECT\",\"comment\":\"补充差异说明\"}").path("data");
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");

        postOk("/api/v1/projects/" + projectId + "/actions", ownerToken,
                "{\"action\":\"SUBMIT\"}");
        acceptProject(pmo, projectId);
        JsonNode detail = getOk("/api/v1/projects/" + projectId, ownerToken).path("data");
        assertThat(detail.path("reviewRound").asInt()).isEqualTo(2);
        assertThat(detail.path("reviewPassCount").asInt()).isEqualTo(1);

        perform(post("/api/v1/projects/{id}/actions", projectId), pmo, 400,
                "{\"action\":\"APPROVE\"}");
        review(tokenOf(expertA), projectId, "PASS", "第二轮通过");
        review(tokenOf(expertB), projectId, "PASS", "第二轮同意");
        JsonNode approved = postOk("/api/v1/projects/" + projectId + "/actions", pmo,
                "{\"action\":\"APPROVE\"}").path("data");
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("UT-G08 非法流转、越权动作与终止")
    void ut08IllegalAndTerminate() throws Exception {
        User owner = createGroupUser("gov-term", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("gov-term-m", owner.getGroupId());
        String ownerToken = tokenOf(owner);
        String pmo = pmoToken();

        long projectId = createSubmittedProject(ownerToken, owner.getGroupId(), owner.getId(),
                member.getId(), uniqueProjectName("gov-term"));
        perform(post("/api/v1/projects/{id}/actions", projectId), ownerToken, 400,
                "{\"action\":\"START\"}");
        perform(post("/api/v1/projects/{id}/actions", projectId), ownerToken, 403,
                "{\"action\":\"ACCEPT\"}");

        acceptProject(pmo, projectId);
        User expertA = createExpert("gov-term-ea");
        User expertB = createExpert("gov-term-eb");
        review(tokenOf(expertA), projectId, "PASS", "通过");
        review(tokenOf(expertB), projectId, "PASS", "通过");
        postOk("/api/v1/projects/" + projectId + "/actions", pmo, "{\"action\":\"APPROVE\"}");
        JsonNode terminated = postOk("/api/v1/projects/" + projectId + "/actions", pmo,
                "{\"action\":\"TERMINATE\",\"comment\":\"方向调整\"}").path("data");
        assertThat(terminated.path("status").asText()).isEqualTo("TERMINATED");
    }
}

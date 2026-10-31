package com.taiji.fund.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 专家评审与待办：本轮唯一、驳回后新轮次、待办一致性、跨组 REVIEWING 可见。
 */
class ReviewTodoTest extends AbstractGovernanceTest {

    @Test
    @DisplayName("UT-G09 本轮不可重复评审，待办评审后消失")
    void ut09ReviewOnceAndTodo() throws Exception {
        User owner = createGroupUser("gov-rev", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("gov-rev-m", owner.getGroupId());
        String ownerToken = tokenOf(owner);
        String pmo = pmoToken();
        User expertA = createExpert("gov-rev-ea");
        User expertB = createExpert("gov-rev-eb");
        String tokenA = tokenOf(expertA);
        String tokenB = tokenOf(expertB);

        long projectId = createSubmittedProject(ownerToken, owner.getGroupId(), owner.getId(),
                member.getId(), uniqueProjectName("gov-rev"));
        acceptProject(pmo, projectId);

        assertThat(todoIds(tokenA)).contains(projectId);
        assertThat(todoIds(pmo)).contains(projectId);

        review(tokenA, projectId, "PASS", "同意");
        assertThat(todoIds(tokenA)).doesNotContain(projectId);
        assertThat(todoIds(tokenB)).contains(projectId);

        perform(post("/api/v1/projects/{id}/reviews", projectId), tokenA, 400,
                "{\"score\":90,\"opinion\":\"再次\",\"result\":\"PASS\"}");

        JsonNode reviews = getOk("/api/v1/projects/" + projectId + "/reviews", pmo).path("data");
        assertThat(reviews.size()).isEqualTo(1);
        assertThat(reviews.get(0).path("result").asText()).isEqualTo("PASS");
        assertThat(reviews.get(0).path("reviewerName").asText()).isNotBlank();
        assertThat(reviews.get(0).path("round").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("UT-G10 驳回重提后新轮次可再次评审；DRAFT 不可评审")
    void ut10NewRoundAndDraftScope() throws Exception {
        User owner = createGroupUser("gov-rev2", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("gov-rev2-m", owner.getGroupId());
        String ownerToken = tokenOf(owner);
        String pmo = pmoToken();
        User expert = createExpert("gov-rev2-e");
        String expertToken = tokenOf(expert);

        long projectId = createSubmittedProject(ownerToken, owner.getGroupId(), owner.getId(),
                member.getId(), uniqueProjectName("gov-rev2"));
        acceptProject(pmo, projectId);
        review(expertToken, projectId, "REVISE", "请补充差异");
        postOk("/api/v1/projects/" + projectId + "/actions", pmo,
                "{\"action\":\"REJECT\",\"comment\":\"补充差异\"}");
        postOk("/api/v1/projects/" + projectId + "/actions", ownerToken,
                "{\"action\":\"SUBMIT\"}");
        acceptProject(pmo, projectId);
        assertThat(todoIds(expertToken)).contains(projectId);
        review(expertToken, projectId, "PASS", "第二轮通过");

        User otherGroupOwner = createGroupUser("gov-rev2-o", groupId("GRP-DATA"));
        long draftId = createDraftId(tokenOf(otherGroupOwner),
                uniqueProjectName("gov-rev2-draft"), otherGroupOwner.getGroupId(),
                otherGroupOwner.getId());
        perform(get("/api/v1/projects/" + draftId), expertToken, 403);
        perform(post("/api/v1/projects/{id}/reviews", draftId), expertToken, 403,
                "{\"score\":80,\"opinion\":\"草稿\",\"result\":\"PASS\"}");
    }

    private java.util.List<Long> todoIds(String token) throws Exception {
        JsonNode items = getOk("/api/v1/tasks/todo", token).path("data").path("items");
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (JsonNode item : items) {
            if ("REVIEW".equals(item.path("action").asText())
                    || "ACCEPT".equals(item.path("action").asText())
                    || "APPROVE".equals(item.path("action").asText())) {
                ids.add(item.path("projectId").asLong());
            }
        }
        return ids;
    }
}

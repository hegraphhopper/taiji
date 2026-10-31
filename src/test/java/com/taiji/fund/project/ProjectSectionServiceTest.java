package com.taiji.fund.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-04、UT-05：结构化申报内容批量保存、排序与事务回滚。
 */
class ProjectSectionServiceTest extends AbstractProjectApiTest {

    @Test
    @DisplayName("UT-04 批量保存五类子表并按 sort 返回，重复保存整体替换")
    void ut04_saveSectionsReplacesAndOrders() throws Exception {
        User owner = createGroupUser("ut04", groupId("GRP-INTELLIGENCE"));
        User member1 = createGroupUser("ut04m1", owner.getGroupId());
        User member2 = createGroupUser("ut04m2", owner.getGroupId());
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut04-项目"), owner.getGroupId(), owner.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("goals", Arrays.asList(
                map("content", "目标A", "sort", 2),
                map("content", "目标B", "sort", 1)));
        body.put("contents", Arrays.asList(
                map("content", "内容A", "sort", 0),
                map("content", "内容B", "sort", 1),
                map("content", "内容C", "sort", 2)));
        body.put("keyTechs", Arrays.asList(
                map("name", "技术A", "description", "说明A", "difficulty", "HIGH", "sort", 0),
                map("name", "技术B", "description", "说明B", "difficulty", "LOW", "sort", 1)));
        body.put("milestones", Arrays.asList(
                map("name", "节点A", "planStart", "2026-11-01", "planEnd", "2027-01-31",
                        "deliverable", "交付A", "weight", 30, "sort", 0),
                map("name", "节点B", "planStart", "2027-02-01", "planEnd", "2027-06-30",
                        "deliverable", "交付B", "weight", 30, "sort", 1),
                map("name", "节点C", "planStart", "2027-07-01", "planEnd", "2027-10-31",
                        "deliverable", "交付C", "weight", 40, "sort", 2)));
        body.put("members", Arrays.asList(
                map("userId", member1.getId(), "memberRole", "CORE", "workload", new BigDecimal("8.00")),
                map("userId", member2.getId(), "memberRole", "PARTICIPANT", "workload", new BigDecimal("4.50"))));

        saveSections(token, projectId, objectMapper.writeValueAsString(body));
        JsonNode detail = detail(token, projectId);

        assertThat(detail.path("goals").size()).isEqualTo(2);
        assertThat(textValues(detail.path("goals"), "content")).containsExactly("目标B", "目标A");
        assertThat(intValues(detail.path("goals"), "sort")).containsExactly(1, 2);

        assertThat(detail.path("contents").size()).isEqualTo(3);
        assertThat(textValues(detail.path("contents"), "content")).containsExactly("内容A", "内容B", "内容C");

        assertThat(detail.path("keyTechs").size()).isEqualTo(2);
        assertThat(textValues(detail.path("keyTechs"), "name")).containsExactly("技术A", "技术B");

        assertThat(detail.path("milestones").size()).isEqualTo(3);
        assertThat(intValues(detail.path("milestones"), "sort")).containsExactly(0, 1, 2);

        assertThat(detail.path("members").size()).isEqualTo(2);
        assertThat(textValues(detail.path("members"), "memberRole")).containsExactlyInAnyOrder("CORE", "PARTICIPANT");

        // 重复保存为整体替换：只保留 1 条目标/内容/节点，关键技术与成员清空
        Map<String, Object> replaced = new LinkedHashMap<>();
        replaced.put("goals", Arrays.asList(map("content", "新目标", "sort", 0)));
        replaced.put("contents", Arrays.asList(map("content", "新内容", "sort", 0)));
        replaced.put("keyTechs", Arrays.asList());
        replaced.put("milestones", Arrays.asList(map("name", "新节点", "planStart", "2026-11-01",
                "planEnd", "2027-03-31", "deliverable", "新交付", "weight", 100, "sort", 0)));
        replaced.put("members", Arrays.asList());
        saveSections(token, projectId, objectMapper.writeValueAsString(replaced));

        JsonNode after = detail(token, projectId);
        assertThat(after.path("goals").size()).isEqualTo(1);
        assertThat(after.path("goals").get(0).path("content").asText()).isEqualTo("新目标");
        assertThat(after.path("contents").size()).isEqualTo(1);
        assertThat(after.path("contents").get(0).path("content").asText()).isEqualTo("新内容");
        assertThat(after.path("keyTechs").size()).isZero();
        assertThat(after.path("milestones").size()).isEqualTo(1);
        assertThat(after.path("milestones").get(0).path("name").asText()).isEqualTo("新节点");
        assertThat(after.path("members").size()).isZero();
    }

    @Test
    @DisplayName("UT-05 子表保存中途失败整体回滚，不残留部分写入")
    void ut05_sectionSaveRollsBackOnFailure() throws Exception {
        User owner = createGroupUser("ut05", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut05m", owner.getGroupId());
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut05-项目"), owner.getGroupId(), owner.getId());

        saveSections(token, projectId, completeSectionsJson(member.getId()));
        JsonNode baseline = detail(token, projectId);
        assertThat(textValues(baseline.path("goals"), "content")).containsExactly("完成多模态缺陷识别算法研究");
        assertThat(textValues(baseline.path("contents"), "content")).containsExactly("构建多模态数据采集与标注平台");
        assertThat(textValues(baseline.path("milestones"), "name")).containsExactly("原型开发");

        // 合法目标 + 非法节点（结束早于开始）：节点校验抛出后整体回滚
        Map<String, Object> invalidMilestone = new LinkedHashMap<>();
        invalidMilestone.put("goals", Arrays.asList(map("content", "被回滚的目标", "sort", 0)));
        invalidMilestone.put("contents", Arrays.asList(map("content", "被回滚的内容", "sort", 0)));
        invalidMilestone.put("milestones", Arrays.asList(map("name", "非法节点", "planStart", "2027-02-01",
                "planEnd", "2027-01-01", "deliverable", "非法交付", "weight", 50, "sort", 0)));
        MvcResult reversed = mockMvc.perform(put("/api/v1/projects/{id}/sections", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidMilestone)))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(reversed).path("message").asText()).contains("结束日期");

        JsonNode afterIllegalMilestone = detail(token, projectId);
        assertThat(textValues(afterIllegalMilestone.path("goals"), "content"))
                .containsExactly("完成多模态缺陷识别算法研究");
        assertThat(textValues(afterIllegalMilestone.path("contents"), "content"))
                .containsExactly("构建多模态数据采集与标注平台");
        assertThat(textValues(afterIllegalMilestone.path("milestones"), "name")).containsExactly("原型开发");

        // 成员人月 0：请求校验失败，同样不产生部分写入
        Map<String, Object> invalidMember = new LinkedHashMap<>();
        invalidMember.put("goals", Arrays.asList(map("content", "被回滚的目标2", "sort", 0)));
        invalidMember.put("members", Arrays.asList(map("userId", member.getId(), "memberRole", "CORE",
                "workload", BigDecimal.ZERO)));
        MvcResult zeroWorkload = mockMvc.perform(put("/api/v1/projects/{id}/sections", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidMember)))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(zeroWorkload).path("message").asText()).contains("投入人月");

        JsonNode afterZeroWorkload = detail(token, projectId);
        assertThat(textValues(afterZeroWorkload.path("goals"), "content"))
                .containsExactly("完成多模态缺陷识别算法研究");
        assertThat(afterZeroWorkload.path("members").size()).isEqualTo(1);
    }
}

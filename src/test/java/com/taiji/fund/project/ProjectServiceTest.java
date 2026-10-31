package com.taiji.fund.project;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-01、UT-02、UT-03、UT-22、UT-23：项目主数据、编码与并发控制。
 */
class ProjectServiceTest extends AbstractProjectApiTest {

    @Test
    @DisplayName("UT-01 创建草稿自动生成编码并保存预算/日期/创建人")
    void ut01_createDraftGeneratesCode() throws Exception {
        User owner = createGroupUser("ut01", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        String name = uniqueProjectName("ut01-项目");

        JsonNode data = createProject(token, projectJson(name, owner.getGroupId(), owner.getId(),
                "1200000.50", "2026-11-01", "2027-10-31", null));

        long id = data.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(data.path("status").asText()).isEqualTo("DRAFT");
        assertThat(data.path("code").asText()).matches("TJJH-\\d{4}-\\d{4}");
        assertThat(data.path("name").asText()).isEqualTo(name);
        assertThat(data.path("budget").decimalValue()).isEqualByComparingTo(new BigDecimal("1200000.50"));
        assertThat(data.path("startDate").asText()).isEqualTo("2026-11-01");
        assertThat(data.path("endDate").asText()).isEqualTo("2027-10-31");

        Project stored = projectMapper.selectById(id);
        assertThat(stored).isNotNull();
        assertThat(stored.getCreatedBy()).isEqualTo(owner.getId());
        assertThat(stored.getStatus()).isEqualTo("DRAFT");
        assertThat(projectMapper.selectCount(Wrappers.<Project>lambdaQuery()
                .eq(Project::getCode, stored.getCode()))).isEqualTo(1L);
    }

    @Test
    @DisplayName("UT-02 手工编码重复返回 400/40020 且不新增项目")
    void ut02_duplicateManualCodeRejected() throws Exception {
        User owner = createGroupUser("ut02", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        String code = uniqueCode("TJJH-MANUAL");
        String firstName = uniqueProjectName("ut02-首个");

        JsonNode first = createProject(token, projectJson(firstName, owner.getGroupId(), owner.getId(),
                "500000.00", "2026-11-01", "2027-10-31", code));

        MvcResult duplicated = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson(uniqueProjectName("ut02-重复"), owner.getGroupId(), owner.getId(),
                                "600000.00", "2026-11-01", "2027-10-31", code)))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = readJson(duplicated);
        assertThat(body.path("code").asInt()).isEqualTo(ErrorCode.PROJECT_CODE_DUPLICATED.getCode());
        assertThat(body.path("message").asText()).contains("编码");
        assertThat(projectMapper.selectCount(Wrappers.<Project>lambdaQuery()
                .eq(Project::getCode, code))).isEqualTo(1L);
        assertThat(projectMapper.selectById(first.path("id").asLong()).getName()).isEqualTo(firstName);
    }

    @Test
    @DisplayName("UT-03 预算非正或开始日期晚于结束日期返回 400 且不写库")
    void ut03_budgetAndDateValidation() throws Exception {
        User owner = createGroupUser("ut03", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        Long group = owner.getGroupId();
        String name = uniqueProjectName("ut03-非法");

        assertCreateRejected(token, projectJson(name, group, owner.getId(),
                "0", "2026-11-01", "2027-10-31", null), "预算");
        assertCreateRejected(token, projectJson(name, group, owner.getId(),
                "-1.00", "2026-11-01", "2027-10-31", null), "预算");
        assertCreateRejected(token, projectJson(name, group, owner.getId(),
                "100.00", "2027-12-01", "2027-01-01", null), "结束日期");

        assertThat(projectMapper.selectCount(Wrappers.<Project>lambdaQuery()
                .eq(Project::getName, name))).isZero();
    }

    @Test
    @DisplayName("UT-22 同一 version 的第二次更新被乐观锁拒绝，不静默覆盖")
    void ut22_optimisticLockRejectsStaleUpdate() throws Exception {
        User owner = createGroupUser("ut22", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        String firstName = uniqueProjectName("ut22-首次");
        long id = createDraftId(token, firstName, owner.getGroupId(), owner.getId());

        String secondName = uniqueProjectName("ut22-第二次");
        updateProject(token, id, projectJson(secondName, owner.getGroupId(), owner.getId(),
                "2000000.00", "2026-11-01", "2027-10-31", null), 0);

        MvcResult stale = mockMvc.perform(put("/api/v1/projects/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withVersion(projectJson(uniqueProjectName("ut22-冲突"), owner.getGroupId(),
                                owner.getId(), "3000000.00", "2026-11-01", "2027-10-31", null), 0)))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(stale).path("message").asText()).contains("刷新");

        Project stored = projectMapper.selectById(id);
        assertThat(stored.getName()).isEqualTo(secondName);
        assertThat(stored.getBudget()).isEqualByComparingTo(new BigDecimal("2000000.00"));
        assertThat(stored.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("UT-23 手工编码保留，自动编码跳过占用序号且保持唯一")
    void ut23_manualAndAutoCodesCoexist() throws Exception {
        User owner = createGroupUser("ut23", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        String manualCode = uniqueCode("TJJH-MANUAL-UT23");

        JsonNode manual = createProject(token, projectJson(uniqueProjectName("ut23-手工"), owner.getGroupId(),
                owner.getId(), "100000.00", "2026-11-01", "2027-10-31", manualCode));
        JsonNode auto1 = createProject(token, projectJson(uniqueProjectName("ut23-自动1"), owner.getGroupId(),
                owner.getId(), "100100.00", "2026-11-01", "2027-10-31", null));
        JsonNode auto2 = createProject(token, projectJson(uniqueProjectName("ut23-自动2"), owner.getGroupId(),
                owner.getId(), "100200.00", "2026-11-01", "2027-10-31", null));

        assertThat(manual.path("code").asText()).isEqualTo(manualCode);
        List<String> codes = java.util.Arrays.asList(
                manual.path("code").asText(), auto1.path("code").asText(), auto2.path("code").asText());
        assertThat(codes).doesNotHaveDuplicates();
        assertThat(auto1.path("code").asText()).matches("TJJH-\\d{4}-\\d{4}");
        assertThat(auto2.path("code").asText()).matches("TJJH-\\d{4}-\\d{4}");
        assertThat(auto1.path("code").asText()).isNotEqualTo(manualCode);
        assertThat(auto2.path("code").asText()).isNotEqualTo(manualCode);
    }

    private void assertCreateRejected(String token, String body, String expectedMessage) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(result).path("message").asText()).contains(expectedMessage);
    }

    private void updateProject(String token, long id, String body, int version) throws Exception {
        mockMvc.perform(put("/api/v1/projects/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withVersion(body, version)))
                .andExpect(status().isOk());
    }

    private String withVersion(String body, int version) throws Exception {
        ObjectNode node = (ObjectNode) objectMapper.readTree(body);
        node.put("version", version);
        return objectMapper.writeValueAsString(node);
    }
}

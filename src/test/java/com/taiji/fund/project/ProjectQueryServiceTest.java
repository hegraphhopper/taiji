package com.taiji.fund.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-12、UT-13：跨组数据范围与列表筛选/排序/分页。
 */
class ProjectQueryServiceTest extends AbstractProjectApiTest {

    @Test
    @DisplayName("UT-12 项目组用户无法查看/修改/删除其他组项目，列表不返回越权数据")
    void ut12_crossGroupAccessDenied() throws Exception {
        Long group1 = groupId("GRP-INTELLIGENCE");
        Long group2 = groupId("GRP-DATA");
        User outsider = createGroupUser("ut12x", group1);
        User group2Owner = createGroupUser("ut12owner", group2);
        String outsiderToken = tokenOf(outsider);
        String adminToken = adminToken();

        String otherName = uniqueProjectName("ut12-外组项目");
        JsonNode other = createProject(adminToken, projectJson(otherName, group2, group2Owner.getId(),
                "800000.00", "2026-11-01", "2027-10-31", null));
        long otherId = other.path("id").asLong();

        MvcResult detailForbidden = mockMvc.perform(get("/api/v1/projects/{id}", otherId)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(readJson(detailForbidden).path("code").asInt()).isEqualTo(ErrorCode.FORBIDDEN.getCode());

        MvcResult updateForbidden = mockMvc.perform(put("/api/v1/projects/{id}", otherId)
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson(uniqueProjectName("ut12-越权改名"), group2, group2Owner.getId(),
                                "900000.00", "2026-11-01", "2027-10-31", null)))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(readJson(updateForbidden).path("code").asInt()).isEqualTo(ErrorCode.FORBIDDEN.getCode());

        MvcResult deleteForbidden = mockMvc.perform(delete("/api/v1/projects/{id}", otherId)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(readJson(deleteForbidden).path("code").asInt()).isEqualTo(ErrorCode.FORBIDDEN.getCode());

        JsonNode list = list(outsiderToken, "keyword=" + otherName);
        assertThat(list.path("total").asLong()).isZero();
        assertThat(list.path("records").size()).isZero();

        // 项目确实存在，且管理员可访问，排除“数据根本不存在”的误解
        assertThat(detail(adminToken, otherId).path("name").asText()).isEqualTo(otherName);
    }

    @Test
    @DisplayName("UT-13 列表关键词/状态/预算筛选、预算倒序与分页正确")
    void ut13_listFiltersSortsPaginates() throws Exception {
        Long group1 = groupId("GRP-INTELLIGENCE");
        User owner = createGroupUser("ut13", group1);
        String token = tokenOf(owner);
        String prefix = "ut13kw" + System.nanoTime();

        createProject(token, projectJson(prefix + "-低", group1, owner.getId(),
                "100000.00", "2026-11-01", "2027-10-31", null));
        createProject(token, projectJson(prefix + "-高", group1, owner.getId(),
                "300000.00", "2026-11-01", "2027-10-31", null));
        createProject(token, projectJson(prefix + "-中", group1, owner.getId(),
                "200000.00", "2026-11-01", "2027-10-31", null));

        // 其他组同前缀项目不应出现在本组用户的结果中
        User otherOwner = createGroupUser("ut13o", groupId("GRP-DATA"));
        createProject(adminToken(), projectJson(prefix + "-外组", otherOwner.getGroupId(), otherOwner.getId(),
                "250000.00", "2026-11-01", "2027-10-31", null));

        JsonNode firstPage = list(token, "keyword=" + prefix
                + "&status=DRAFT&status=SUBMITTED&sortBy=budget&sortOrder=desc&page=1&size=2");
        assertThat(firstPage.path("total").asLong()).isEqualTo(3);
        assertThat(firstPage.path("page").asLong()).isEqualTo(1);
        assertThat(firstPage.path("size").asLong()).isEqualTo(2);
        assertThat(firstPage.path("records").size()).isEqualTo(2);
        assertBudgets(firstPage.path("records"), "300000.00", "200000.00");

        JsonNode secondPage = list(token, "keyword=" + prefix
                + "&status=DRAFT&sortBy=budget&sortOrder=desc&page=2&size=2");
        assertThat(secondPage.path("total").asLong()).isEqualTo(3);
        assertThat(secondPage.path("records").size()).isEqualTo(1);
        assertBudgets(secondPage.path("records"), "100000.00");

        JsonNode budgetRange = list(token, "keyword=" + prefix
                + "&budgetMin=150000&budgetMax=350000&sortBy=budget&sortOrder=desc");
        assertThat(budgetRange.path("total").asLong()).isEqualTo(2);
        List<BigDecimal> rangeBudgets = budgets(budgetRange.path("records"));
        assertThat(rangeBudgets).hasSize(2);
        assertThat(rangeBudgets.get(0)).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(rangeBudgets.get(1)).isEqualByComparingTo(new BigDecimal("200000.00"));

        JsonNode submittedOnly = list(token, "keyword=" + prefix + "&status=SUBMITTED");
        assertThat(submittedOnly.path("total").asLong()).isZero();
    }

    private List<BigDecimal> budgets(JsonNode records) {
        List<BigDecimal> budgets = new ArrayList<>();
        for (JsonNode record : records) {
            budgets.add(record.path("budget").decimalValue());
        }
        return budgets;
    }

    private void assertBudgets(JsonNode records, String... expected) {
        List<BigDecimal> actual = budgets(records);
        assertThat(actual).hasSize(expected.length);
        for (int index = 0; index < expected.length; index++) {
            assertThat(actual.get(index)).isEqualByComparingTo(new BigDecimal(expected[index]));
        }
    }
}

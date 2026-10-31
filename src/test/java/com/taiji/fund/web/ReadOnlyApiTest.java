package com.taiji.fund.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-10、UT-11、UT-16：字典聚合、组织/技术方向只读接口与健康检查。
 */
class ReadOnlyApiTest extends AbstractApiTest {

    @Test
    @DisplayName("UT-10 字典聚合接口返回基金类型/项目状态/角色/技术方向")
    void ut10_dictsAggregateSeedData() throws Exception {
        String token = loginToken("user01", "User@12345");

        MvcResult result = mockMvc.perform(get("/api/v1/dicts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = readJson(result).path("data");

        JsonNode fundTypes = data.path("fundTypes");
        assertThat(fundTypes.size()).isEqualTo(3);
        List<String> fundTypeCodes = codes(fundTypes);
        assertThat(fundTypeCodes).containsExactly("PLATFORM", "INNOVATION", "OTHER");

        assertThat(data.path("projectStatuses").size()).isEqualTo(8);
        assertThat(codes(data.path("roles"))).containsExactlyInAnyOrder("ADMIN", "PMO", "GROUP_USER", "EXPERT");

        JsonNode techDirections = data.path("techDirections");
        assertThat(techDirections.size()).isEqualTo(12);
        JsonNode firstDirection = techDirections.get(0);
        assertThat(firstDirection.path("code").asText()).isEqualTo("TD-001");
        assertThat(firstDirection.path("name").asText()).isEqualTo("人工智能");
        assertThat(firstDirection.path("keywords").isArray()).isTrue();
        assertThat(firstDirection.path("keywords").size()).isGreaterThanOrEqualTo(3);
        assertThat(textValues(firstDirection.path("keywords"))).contains("机器学习");
    }

    @Test
    @DisplayName("UT-11 项目组/技术方向只读列表按排序返回，且不暴露写接口")
    void ut11_groupAndTechDirectionListsAreReadOnly() throws Exception {
        String token = loginToken("user01", "User@12345");

        MvcResult groupResult = mockMvc.perform(get("/api/v1/groups").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode groups = readJson(groupResult).path("data");
        assertThat(groups.size()).isEqualTo(8);
        assertThat(groups.get(0).path("name").asText()).isEqualTo("智能计算项目组");
        assertThat(groups.get(0).path("code").asText()).isEqualTo("GRP-INTELLIGENCE");
        assertThat(sorts(groups)).isSorted();

        MvcResult directionResult = mockMvc.perform(get("/api/v1/tech-directions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode directions = readJson(directionResult).path("data");
        assertThat(directions.size()).isEqualTo(12);
        assertThat(sorts(directions)).isSorted();

        int postStatus = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新增项目组\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(postStatus).isIn(403, 405);

        int putStatus = mockMvc.perform(put("/api/v1/tech-directions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新增技术方向\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(putStatus).isIn(403, 405);
    }

    @Test
    @DisplayName("UT-16 健康检查匿名可用且数据库状态为 UP")
    void ut16_healthEndpointReportsUp() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = readJson(result);
        JsonNode data = body.path("data");

        assertThat(body.path("code").asInt()).isZero();
        assertThat(data.path("status").asText()).isEqualTo("UP");
        assertThat(data.path("db").asText()).isEqualTo("UP");
        assertThat(data.path("profile").asText()).contains("demo");
        assertThat(data.path("version").asText()).isEqualTo("1.0.0");
        assertThat(data.path("app").asText()).isNotBlank();
    }

    private List<String> codes(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.path("code").asText());
        }
        return values;
    }

    private List<String> textValues(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.asText());
        }
        return values;
    }

    private List<Integer> sorts(JsonNode array) {
        List<Integer> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.path("sort").asInt());
        }
        return values;
    }
}

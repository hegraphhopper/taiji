package com.taiji.fund.project;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.AbstractApiTest;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.project.mapper.ProjectApprovalLogMapper;
import com.taiji.fund.project.mapper.ProjectDocumentMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import com.taiji.fund.system.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A2 项目与申报书管理测试基类：统一项目组/技术方向查找、测试账号、项目与结构化内容 JSON 构造。
 */
public abstract class AbstractProjectApiTest extends AbstractApiTest {

    protected static final String PASSWORD = "Passw0rd!";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String PMO_PASSWORD = "Pmo@12345";

    private static final AtomicInteger PROJECT_SEQUENCE = new AtomicInteger();

    @Autowired
    protected GroupMapper groupMapper;

    @Autowired
    protected TechDirectionMapper techDirectionMapper;

    @Autowired
    protected ProjectMapper projectMapper;

    @Autowired
    protected ProjectDocumentMapper projectDocumentMapper;

    @Autowired
    protected ProjectApprovalLogMapper approvalLogMapper;

    protected Group group(String code) {
        Group group = groupMapper.selectOne(Wrappers.<Group>lambdaQuery().eq(Group::getCode, code));
        if (group == null) {
            throw new IllegalStateException("缺少项目组种子数据: " + code);
        }
        return group;
    }

    protected Long groupId(String code) {
        return group(code).getId();
    }

    protected Long firstTechDirectionId() {
        List<TechDirection> directions = techDirectionMapper.selectList(
                Wrappers.<TechDirection>lambdaQuery().orderByAsc(TechDirection::getId));
        if (directions.isEmpty()) {
            throw new IllegalStateException("缺少技术方向种子数据");
        }
        return directions.get(0).getId();
    }

    protected User createGroupUser(String prefix, Long groupId) {
        return createUser(prefix, PASSWORD, UserStatus.ENABLED, groupId, RoleCode.GROUP_USER, 0);
    }

    protected String tokenOf(User user) throws Exception {
        return loginToken(user.getUsername(), PASSWORD);
    }

    protected String adminToken() throws Exception {
        return loginToken("admin", ADMIN_PASSWORD);
    }

    protected String pmoToken() throws Exception {
        return loginToken("pmo01", PMO_PASSWORD);
    }

    protected String uniqueProjectName(String prefix) {
        return prefix + "-" + System.nanoTime() + "-" + PROJECT_SEQUENCE.incrementAndGet();
    }

    protected String uniqueCode(String prefix) {
        return prefix + "-" + System.nanoTime() + "-" + PROJECT_SEQUENCE.incrementAndGet();
    }

    protected String projectJson(String name, Long groupId, Long ownerId, String budget,
                                 String startDate, String endDate, String code) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (code != null) {
            body.put("code", code);
        }
        body.put("fundType", "PLATFORM");
        body.put("groupId", groupId);
        body.put("ownerId", ownerId);
        body.put("techDirectionId", firstTechDirectionId());
        body.put("budget", new BigDecimal(budget));
        body.put("startDate", startDate);
        body.put("endDate", endDate);
        body.put("summary", "自动化测试项目摘要");
        body.put("expectedOutcome", "自动化测试预期成果");
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode createProject(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected long createDraftId(String token, String name, Long groupId, Long ownerId) throws Exception {
        String budget = "1000000.00";
        JsonNode data = createProject(token,
                projectJson(name, groupId, ownerId, budget, "2026-11-01", "2027-10-31", null));
        return data.path("id").asLong();
    }

    protected String completeSectionsJson(Long memberId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("goals", Arrays.asList(map("content", "完成多模态缺陷识别算法研究", "sort", 0)));
        body.put("contents", Arrays.asList(map("content", "构建多模态数据采集与标注平台", "sort", 0)));
        body.put("keyTechs", Arrays.asList(map("name", "多模态特征融合", "description", "跨模态特征对齐",
                "difficulty", "HIGH", "sort", 0)));
        body.put("milestones", Arrays.asList(map("name", "原型开发", "planStart", "2026-11-01",
                "planEnd", "2027-02-28", "deliverable", "原型系统", "weight", 40, "sort", 0)));
        body.put("members", Arrays.asList(map("userId", memberId, "memberRole", "LEADER",
                "workload", new BigDecimal("6.00"))));
        return objectMapper.writeValueAsString(body);
    }

    protected void saveSections(String token, long projectId, String body) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/projects/{id}/sections", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    protected JsonNode submit(String token, long projectId) throws Exception {
        return performAction(token, projectId, "SUBMIT", null, null);
    }

    protected JsonNode performAction(String token, long projectId, String action, String comment,
                                     Integer version) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", action);
        if (comment != null) {
            body.put("comment", comment);
        }
        if (version != null) {
            body.put("version", version);
        }
        MvcResult result = mockMvc.perform(post("/api/v1/projects/{id}/actions", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected JsonNode detail(String token, long projectId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected JsonNode list(String token, String queryString) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/projects?" + queryString)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    protected static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    protected static List<Integer> intValues(JsonNode array, String field) {
        List<Integer> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.path(field).asInt());
        }
        return values;
    }

    protected static List<String> textValues(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.path(field).asText());
        }
        return values;
    }
}

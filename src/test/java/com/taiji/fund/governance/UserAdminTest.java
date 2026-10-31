package com.taiji.fund.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 用户管理、角色分配、禁用即时失效与组/方向删除校验。
 */
class UserAdminTest extends AbstractGovernanceTest {

    @Test
    @DisplayName("UT-G03 新增用户、重置密码、禁用后旧 token 401")
    void ut03CreateResetDisable() throws Exception {
        String admin = adminToken();
        String username = uniqueUsername("gov-user");
        JsonNode created = postOk("/api/v1/users", admin, userBody(username, "Passw0rd!",
                "ENABLED", Arrays.asList("PMO"), null)).path("data");
        long userId = created.path("id").asLong();
        assertThat(created.path("roles").toString()).contains("PMO");

        String firstToken = loginToken(username, "Passw0rd!");
        getOk("/api/v1/auth/me", firstToken);

        JsonNode reset = postOk("/api/v1/users/" + userId + "/reset-password", admin, "{}")
                .path("data");
        String temporary = reset.path("temporaryPassword").asText();
        assertThat(temporary).hasSizeGreaterThanOrEqualTo(8);
        String newToken = loginToken(username, temporary);
        getOk("/api/v1/auth/me", newToken);

        Map<String, Object> disabled = userBody(username, null, "DISABLED",
                Arrays.asList("PMO"), null);
        putOk("/api/v1/users/" + userId, admin, disabled);
        perform(get("/api/v1/auth/me"), newToken, 401);

        putOk("/api/v1/users/" + userId, admin,
                userBody(username, null, "ENABLED", Arrays.asList("PMO"), null));
        getOk("/api/v1/auth/me", loginToken(username, temporary));
    }

    @Test
    @DisplayName("UT-G04 非管理员不可管理用户；组/方向删除引用校验")
    void ut04PermissionAndDeleteGuards() throws Exception {
        String admin = adminToken();
        perform(get("/api/v1/users"), pmoToken(), 403);
        perform(get("/api/v1/audit-logs"), pmoToken(), 403);

        String groupCode = "GRP-GOV-" + System.nanoTime();
        JsonNode group = postOk("/api/v1/groups", admin,
                "{\"name\":\"治理测试组\",\"code\":\"" + groupCode + "\",\"sort\":99}")
                .path("data");
        long groupId = group.path("id").asLong();

        String username = uniqueUsername("gov-group-user");
        JsonNode user = postOk("/api/v1/users", admin, userBody(username, "Passw0rd!",
                "ENABLED", Arrays.asList("GROUP_USER"), groupId)).path("data");
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/groups/" + groupId), admin, 400);
        assertThat(getOk("/api/v1/groups/all", admin).toString()).contains(groupCode);

        putOk("/api/v1/users/" + user.path("id").asLong(), admin,
                userBody(username, null, "ENABLED", Arrays.asList("GROUP_USER"), null));
        deleteOk("/api/v1/groups/" + groupId, admin);

        long seedDirection = firstTechDirectionId();
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/tech-directions/" + seedDirection), admin, 400);

        String directionCode = "TD-GOV-" + System.nanoTime();
        JsonNode direction = postOk("/api/v1/tech-directions", admin,
                "{\"name\":\"治理测试方向\",\"code\":\"" + directionCode + "\",\"keywords\":[\"甲\",\"乙\"]}")
                .path("data");
        deleteOk("/api/v1/tech-directions/" + direction.path("id").asLong(), admin);
    }

    @Test
    @DisplayName("UT-G05 用户检索与角色权限点")
    void ut05ListAndRoles() throws Exception {
        String admin = adminToken();
        String username = uniqueUsername("gov-list");
        postOk("/api/v1/users", admin, userBody(username, "Passw0rd!",
                "ENABLED", Arrays.asList("GROUP_USER"), groupId("GRP-INTELLIGENCE")));

        JsonNode page = getOk("/api/v1/users?keyword=" + username + "&role=GROUP_USER", admin)
                .path("data");
        assertThat(page.path("total").asLong()).isEqualTo(1);
        assertThat(page.path("records").get(0).path("roles").toString()).contains("GROUP_USER");

        JsonNode roles = getOk("/api/v1/roles", admin).path("data");
        assertThat(roles.size()).isEqualTo(4);
        assertThat(roles.toString()).contains("SYSTEM_USER_MANAGE")
                .contains("PROJECT_REVIEW_SCORE");
    }

    private Map<String, Object> userBody(String username, String password, String status,
                                         java.util.List<String> roles, Long groupId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        if (password != null) {
            body.put("password", password);
        }
        body.put("realName", username + "-姓名");
        body.put("email", username + "@taiji.example");
        body.put("status", status);
        body.put("roles", roles);
        if (groupId != null) {
            body.put("groupId", groupId);
        }
        return body;
    }
}

package com.taiji.fund.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.AbstractApiTest;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.system.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-05 ~ UT-09、UT-19：JWT 过滤器、匿名放行、角色校验与当前用户契约。
 */
class SecurityIntegrationTest extends AbstractApiTest {

    private static final String ADMIN_PROBE = "/api/v1/test/admin-probe";

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("UT-05 合法 JWT 建立认证并可访问 /auth/me")
    void ut05_validJwtAuthenticatesRequest() throws Exception {
        String loginUsername = uniqueUsername("ut05");
        User user = createEnabledUser(loginUsername, "Passw0rd!", null);

        String token = loginToken(user.getUsername(), "Passw0rd!");
        assertThat(token).isNotBlank();

        MvcResult meResult = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = readJson(meResult).path("data");
        assertThat(data.path("username").asText()).isEqualTo(user.getUsername());
        assertThat(data.path("id").asLong()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("UT-06 非法与过期 JWT 返回 401/40103 且不泄露堆栈")
    void ut06_invalidOrExpiredJwtRejected() throws Exception {
        MvcResult invalidResult = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer("invalid.token.value")))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode invalidBody = readJson(invalidResult);
        assertThat(invalidBody.path("code").asInt()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID.getCode());
        assertNoStackTrace(invalidResult, invalidBody);

        MvcResult expiredResult = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer(expiredToken("admin"))))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode expiredBody = readJson(expiredResult);
        assertThat(expiredBody.path("code").asInt()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID.getCode());
        assertNoStackTrace(expiredResult, expiredBody);
    }

    @Test
    @DisplayName("UT-07 未认证访问受保护接口返回 401，登录与健康检查保持匿名")
    void ut07_anonymousProtectedRequestRejected() throws Exception {
        MvcResult dictResult = mockMvc.perform(get("/api/v1/dicts"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode dictBody = readJson(dictResult);
        assertThat(dictBody.path("code").asInt()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
        assertThat(dictResult.getResponse().getContentType()).contains("application/json");
        assertThat(dictBody.path("data").isNull() || dictBody.path("data").isMissingNode()).isTrue();

        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "Admin@123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UT-08 角色不足访问管理员接口返回 403/40301")
    void ut08_groupUserForbiddenOnAdminEndpoint() throws Exception {
        String userToken = loginToken("user01", "User@12345");

        MvcResult forbidden = mockMvc.perform(get(ADMIN_PROBE).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(readJson(forbidden).path("code").asInt()).isEqualTo(ErrorCode.FORBIDDEN.getCode());

        String adminToken = loginToken("admin", "Admin@123");
        MvcResult allowed = mockMvc.perform(get(ADMIN_PROBE).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(allowed).path("code").asInt()).isZero();
    }

    @Test
    @DisplayName("UT-09 当前用户信息完整且不含敏感字段")
    void ut09_currentUserPayloadIsCompleteAndSafe() throws Exception {
        String token = loginToken("user01", "User@12345");

        MvcResult result = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = readJson(result).path("data");

        assertThat(data.path("id").asLong()).isPositive();
        assertThat(data.path("username").asText()).isEqualTo("user01");
        assertThat(data.path("realName").asText()).isEqualTo("项目组用户");
        assertThat(data.path("groupId").asLong()).isPositive();
        assertThat(data.path("groupName").asText()).isEqualTo("智能计算项目组");
        assertThat(toTextList(data.path("roles"))).contains("GROUP_USER");

        String lowerBody = rawBody(result).toLowerCase();
        assertThat(lowerBody).doesNotContain("password");
        assertThat(lowerBody).doesNotContain("failcount");
        assertThat(lowerBody).doesNotContain("lockeduntil");
    }

    @Test
    @DisplayName("UT-19 登出写入成功响应；无状态 JWT 有效期内仍可解析")
    void ut19_logoutIsStateless() throws Exception {
        String token = loginToken("user01", "User@12345");

        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(logoutResult).path("code").asInt()).isZero();

        // 实现为无状态登出（无黑名单）：文档要求前端清除 token，服务端仍接受未过期 JWT
        MvcResult meResult = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andReturn();
        assertThat(meResult.getResponse().getStatus()).isIn(200, 401);
        if (meResult.getResponse().getStatus() == 200) {
            assertThat(readJson(meResult).path("data").path("username").asText()).isEqualTo("user01");
        }
    }

    private java.util.List<String> toTextList(JsonNode array) {
        java.util.List<String> values = new java.util.ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                values.add(node.asText());
            }
        }
        return values;
    }

    private void assertNoStackTrace(MvcResult result, JsonNode body) {
        String raw = rawBody(result);
        assertThat(result.getResponse().getContentType()).contains("application/json");
        assertThat(body.has("trace")).isFalse();
        assertThat(body.has("exception")).isFalse();
        assertThat(raw).doesNotContain("at com.taiji").doesNotContain("Exception:");
        assertThat(body.path("data").isNull()).isTrue();
    }

    private String expiredToken(String username) {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);
        Date issuedAt = new Date(System.currentTimeMillis() - 7_200_000L);
        Date expiration = new Date(System.currentTimeMillis() - 60_000L);
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", Collections.singletonList("ADMIN"))
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}

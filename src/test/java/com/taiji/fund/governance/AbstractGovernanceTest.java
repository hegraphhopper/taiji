package com.taiji.fund.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.project.AbstractProjectApiTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C1 治理测试基类：统一 JSON 请求与审批夹具。
 */
public abstract class AbstractGovernanceTest extends AbstractProjectApiTest {

    protected JsonNode getOk(String url, String token) throws Exception {
        return perform(get(url), token, 200);
    }

    protected JsonNode postOk(String url, String token, Object body) throws Exception {
        return perform(post(url), token, 200, body);
    }

    protected JsonNode putOk(String url, String token, Object body) throws Exception {
        return perform(put(url), token, 200, body);
    }

    protected void deleteOk(String url, String token) throws Exception {
        perform(delete(url), token, 200);
    }

    protected JsonNode perform(MockHttpServletRequestBuilder builder, String token,
                               int expectedStatus, Object body) throws Exception {
        builder.header("Authorization", bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON)
                    .content(body instanceof String ? (String) body
                            : objectMapper.writeValueAsString(body));
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return readJson(result);
    }

    protected JsonNode perform(MockHttpServletRequestBuilder builder, String token,
                               int expectedStatus) throws Exception {
        return perform(builder, token, expectedStatus, null);
    }

    protected void review(String token, long projectId, String result, String opinion) throws Exception {
        postOk("/api/v1/projects/" + projectId + "/reviews", token,
                "{\"score\":88,\"opinion\":\"" + opinion + "\",\"result\":\"" + result + "\"}");
    }

    protected long createSubmittedProject(String ownerToken, Long groupId, Long ownerId,
                                          Long memberId, String name) throws Exception {
        long projectId = createDraftId(ownerToken, name, groupId, ownerId);
        saveSections(ownerToken, projectId, completeSectionsJson(memberId));
        submit(ownerToken, projectId);
        return projectId;
    }

    protected long acceptProject(String pmoToken, long projectId) throws Exception {
        JsonNode data = postOk("/api/v1/projects/" + projectId + "/actions", pmoToken,
                "{\"action\":\"ACCEPT\",\"comment\":\"形式审查通过\"}").path("data");
        assertThat(data.path("status").asText()).isEqualTo("REVIEWING");
        return projectId;
    }

    protected void assertStatus(String token, long projectId, String status) throws Exception {
        assertThat(getOk("/api/v1/projects/" + projectId, token).path("data")
                .path("status").asText()).isEqualTo(status);
    }
}

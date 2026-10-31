package com.taiji.fund.project;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.ProjectApprovalLog;
import com.taiji.fund.system.audit.AuditLog;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-06 ~ UT-11、UT-24：提交完整性、状态闭环、角色权限与审批轨迹。
 */
class ProjectWorkflowServiceTest extends AbstractProjectApiTest {

    @Test
    @DisplayName("UT-06 提交前缺少目标/内容/节点被拦截，状态仍为草稿且无轨迹")
    void ut06_submitRequiresCompleteSections() throws Exception {
        User owner = createGroupUser("ut06", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut06-项目"), owner.getGroupId(), owner.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{id}/actions", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUBMIT\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(result).path("message").asText()).contains("项目目标");

        assertThat(detail(token, projectId).path("status").asText()).isEqualTo("DRAFT");
        assertThat(approvalLogMapper.selectCount(Wrappers.<ProjectApprovalLog>lambdaQuery()
                .eq(ProjectApprovalLog::getProjectId, projectId))).isZero();
    }

    @Test
    @DisplayName("UT-07 提交成功写入 submit_time、审批轨迹与审计")
    void ut07_submitWritesSubmitTimeAndLog() throws Exception {
        User owner = createGroupUser("ut07", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut07m", owner.getGroupId());
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut07-项目"), owner.getGroupId(), owner.getId());
        saveSections(token, projectId, completeSectionsJson(member.getId()));

        JsonNode submitted = submit(token, projectId);
        assertThat(submitted.path("status").asText()).isEqualTo("SUBMITTED");
        assertThat(submitted.path("submitTime").asText()).isNotBlank();

        JsonNode logs = logs(token, projectId);
        assertThat(logs.size()).isEqualTo(1);
        assertThat(logs.get(0).path("fromStatus").asText()).isEqualTo("DRAFT");
        assertThat(logs.get(0).path("toStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(logs.get(0).path("action").asText()).isEqualTo("SUBMIT");
        assertThat(logs.get(0).path("operatorId").asLong()).isEqualTo(owner.getId());

        List<AuditLog> audits = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getUserId, owner.getId())
                .eq(AuditLog::getAction, "PROJECT_SUBMIT")
                .eq(AuditLog::getResult, "SUCCESS"));
        assertThat(audits).isNotEmpty();
    }

    @Test
    @DisplayName("UT-08 撤回后可修改并重新提交，submit_time 保留首次值")
    void ut08_withdrawAndResubmitKeepsFirstSubmitTime() throws Exception {
        User owner = createGroupUser("ut08", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut08m", owner.getGroupId());
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut08-项目"), owner.getGroupId(), owner.getId());
        saveSections(token, projectId, completeSectionsJson(member.getId()));

        JsonNode firstSubmit = submit(token, projectId);
        String firstSubmitTime = firstSubmit.path("submitTime").asText();
        assertThat(firstSubmitTime).isNotBlank();

        JsonNode withdrawn = performAction(token, projectId, "WITHDRAW", "退回修改", null);
        assertThat(withdrawn.path("status").asText()).isEqualTo("DRAFT");

        String newName = uniqueProjectName("ut08-修改后");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/projects/{id}", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson(newName, owner.getGroupId(), owner.getId(),
                                "1200000.00", "2026-11-01", "2027-10-31", null)))
                .andExpect(status().isOk());

        JsonNode secondSubmit = submit(token, projectId);
        assertThat(secondSubmit.path("status").asText()).isEqualTo("SUBMITTED");
        assertThat(secondSubmit.path("submitTime").asText()).isEqualTo(firstSubmitTime);

        JsonNode logs = logs(token, projectId);
        assertThat(logs.size()).isEqualTo(3);
        assertThat(logs.get(1).path("action").asText()).isEqualTo("WITHDRAW");
        assertThat(logs.get(2).path("action").asText()).isEqualTo("SUBMIT");
    }

    @Test
    @DisplayName("UT-09 管理员确认与驳回；驳回意见必填")
    void ut09_approveAndReject() throws Exception {
        User owner = createGroupUser("ut09", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut09m", owner.getGroupId());
        String token = tokenOf(owner);
        String pmoToken = pmoToken();

        long approveId = createDraftId(token, uniqueProjectName("ut09-立项"), owner.getGroupId(), owner.getId());
        saveSections(token, approveId, completeSectionsJson(member.getId()));
        submit(token, approveId);
        JsonNode approved = performAction(pmoToken, approveId, "APPROVE", "同意立项", null);
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");

        long rejectId = createDraftId(token, uniqueProjectName("ut09-驳回"), owner.getGroupId(), owner.getId());
        saveSections(token, rejectId, completeSectionsJson(member.getId()));
        submit(token, rejectId);

        MvcResult noComment = mockMvc.perform(post("/api/v1/projects/{id}/actions", rejectId)
                        .header("Authorization", bearer(pmoToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REJECT\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(noComment).path("message").asText()).contains("驳回必须填写意见");

        JsonNode rejected = performAction(pmoToken, rejectId, "REJECT", "材料不完整", null);
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");

        JsonNode logs = logs(token, rejectId);
        assertThat(logs.size()).isEqualTo(2);
        assertThat(logs.get(1).path("fromStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(logs.get(1).path("toStatus").asText()).isEqualTo("REJECTED");
        assertThat(logs.get(1).path("comment").asText()).isEqualTo("材料不完整");
    }

    @Test
    @DisplayName("UT-10 草稿直接确认立项返回 400/40010，状态不变")
    void ut10_illegalTransitionRejected() throws Exception {
        User owner = createGroupUser("ut10", groupId("GRP-INTELLIGENCE"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut10-项目"), owner.getGroupId(), owner.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{id}/actions", projectId)
                        .header("Authorization", bearer(pmoToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(result).path("code").asInt())
                .isEqualTo(ErrorCode.PROJECT_ILLEGAL_TRANSITION.getCode());

        assertThat(detail(token, projectId).path("status").asText()).isEqualTo("DRAFT");
        assertThat(approvalLogMapper.selectCount(Wrappers.<ProjectApprovalLog>lambdaQuery()
                .eq(ProjectApprovalLog::getProjectId, projectId))).isZero();
    }

    @Test
    @DisplayName("UT-11 项目组用户确认立项返回 403，仅 PMO 可改变状态")
    void ut11_approverRoleEnforced() throws Exception {
        User owner = createGroupUser("ut11", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut11m", owner.getGroupId());
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut11-项目"), owner.getGroupId(), owner.getId());
        saveSections(token, projectId, completeSectionsJson(member.getId()));
        submit(token, projectId);

        MvcResult forbidden = mockMvc.perform(post("/api/v1/projects/{id}/actions", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(readJson(forbidden).path("code").asInt()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
        assertThat(detail(token, projectId).path("status").asText()).isEqualTo("SUBMITTED");

        JsonNode approved = performAction(pmoToken(), projectId, "APPROVE", "基金办确认", null);
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("UT-24 审批轨迹按时间升序返回，越权查询 403")
    void ut24_approvalLogsAscendingAndAccessControlled() throws Exception {
        User owner = createGroupUser("ut24", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut24m", owner.getGroupId());
        User outsider = createGroupUser("ut24x", groupId("GRP-DATA"));
        String token = tokenOf(owner);
        long projectId = createDraftId(token, uniqueProjectName("ut24-项目"), owner.getGroupId(), owner.getId());
        saveSections(token, projectId, completeSectionsJson(member.getId()));

        submit(token, projectId);
        performAction(pmoToken(), projectId, "REJECT", "补充材料", null);
        performAction(token, projectId, "SUBMIT", null, null);

        JsonNode logs = logs(token, projectId);
        assertThat(logs.size()).isEqualTo(3);
        assertThat(logs.get(0).path("fromStatus").asText()).isEqualTo("DRAFT");
        assertThat(logs.get(0).path("toStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(logs.get(1).path("action").asText()).isEqualTo("REJECT");
        assertThat(logs.get(1).path("comment").asText()).isEqualTo("补充材料");
        assertThat(logs.get(2).path("fromStatus").asText()).isEqualTo("REJECTED");
        assertThat(logs.get(2).path("toStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(logs.get(0).path("id").asLong()).isLessThan(logs.get(1).path("id").asLong());
        assertThat(logs.get(1).path("id").asLong()).isLessThan(logs.get(2).path("id").asLong());

        MvcResult forbidden = mockMvc.perform(get("/api/v1/projects/{id}/logs", projectId)
                        .header("Authorization", bearer(tokenOf(outsider))))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(readJson(forbidden).path("code").asInt()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    private JsonNode logs(String token, long projectId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/projects/{id}/logs", projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }
}

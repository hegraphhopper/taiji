package com.taiji.fund.project;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.system.audit.AuditLog;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-14、UT-15：草稿删除限制与写操作审计。
 */
class ProjectDeleteAndAuditTest extends AbstractProjectApiTest {

    @Test
    @DisplayName("UT-14 仅草稿可逻辑删除，已提交项目删除返回 400，删除动作写审计")
    void ut14_deleteDraftOnly() throws Exception {
        User owner = createGroupUser("ut14", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut14m", owner.getGroupId());
        String token = tokenOf(owner);

        String draftName = uniqueProjectName("ut14-草稿");
        long draftId = createDraftId(token, draftName, owner.getGroupId(), owner.getId());

        MvcResult deleted = mockMvc.perform(delete("/api/v1/projects/{id}", draftId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(deleted).path("code").asInt()).isZero();

        MvcResult gone = mockMvc.perform(get("/api/v1/projects/{id}", draftId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andReturn();
        assertThat(readJson(gone).path("code").asInt()).isEqualTo(ErrorCode.PROJECT_NOT_FOUND.getCode());
        assertThat(list(token, "keyword=" + draftName).path("total").asLong()).isZero();

        String submittedName = uniqueProjectName("ut14-已提交");
        long submittedId = createDraftId(token, submittedName, owner.getGroupId(), owner.getId());
        saveSections(token, submittedId, completeSectionsJson(member.getId()));
        submit(token, submittedId);

        MvcResult rejected = mockMvc.perform(delete("/api/v1/projects/{id}", submittedId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(readJson(rejected).path("code").asInt()).isEqualTo(ErrorCode.PROJECT_NOT_EDITABLE.getCode());
        assertThat(projectMapper.selectById(submittedId).getStatus()).isEqualTo("SUBMITTED");

        List<AuditLog> deleteAudits = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getUserId, owner.getId())
                .eq(AuditLog::getAction, "PROJECT_DELETE")
                .eq(AuditLog::getResult, "SUCCESS"));
        assertThat(deleteAudits).anyMatch(log -> log.getDetail() != null
                && log.getDetail().contains(String.valueOf(draftId)));
    }

    @Test
    @DisplayName("UT-15 创建/保存内容/提交/确认均写审计，失败动作记录 FAIL 且不含敏感信息")
    void ut15_writeOperationsAudited() throws Exception {
        User owner = createGroupUser("ut15", groupId("GRP-INTELLIGENCE"));
        User member = createGroupUser("ut15m", owner.getGroupId());
        String token = tokenOf(owner);
        String pmoToken = pmoToken();

        long projectId = createDraftId(token, uniqueProjectName("ut15-项目"), owner.getGroupId(), owner.getId());
        saveSections(token, projectId, completeSectionsJson(member.getId()));
        submit(token, projectId);
        performAction(pmoToken, projectId, "APPROVE", "审计用例确认", null);

        assertActionSucceeded(owner.getId(), "PROJECT_CREATE");
        assertActionSucceeded(owner.getId(), "PROJECT_SECTIONS_SAVE");
        assertActionSucceeded(owner.getId(), "PROJECT_SUBMIT");

        List<AuditLog> approveAudits = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, "PROJECT_APPROVE")
                .eq(AuditLog::getResult, "SUCCESS"));
        assertThat(approveAudits).anyMatch(log -> log.getDetail() != null
                && log.getDetail().contains(String.valueOf(projectId)));

        long draftId = createDraftId(token, uniqueProjectName("ut15-非法确认"), owner.getGroupId(), owner.getId());
        mockMvc.perform(post("/api/v1/projects/{id}/actions", draftId)
                        .header("Authorization", bearer(pmoToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest());

        List<AuditLog> failedAudits = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, "PROJECT_APPROVE")
                .eq(AuditLog::getResult, "FAIL"));
        assertThat(failedAudits).anyMatch(log -> log.getDetail() != null
                && log.getDetail().contains(String.valueOf(draftId)));

        for (String action : Arrays.asList("PROJECT_CREATE", "PROJECT_SECTIONS_SAVE",
                "PROJECT_SUBMIT", "PROJECT_APPROVE")) {
            List<AuditLog> audits = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                    .eq(AuditLog::getAction, action));
            assertThat(audits).isNotEmpty();
            for (AuditLog audit : audits) {
                String detail = audit.getDetail() == null ? "" : audit.getDetail().toLowerCase();
                assertThat(detail).doesNotContain("password").doesNotContain("token");
            }
        }

        Project stored = projectMapper.selectById(projectId);
        assertThat(stored.getStatus()).isEqualTo("APPROVED");
    }

    private void assertActionSucceeded(Long userId, String action) {
        List<AuditLog> audits = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getUserId, userId)
                .eq(AuditLog::getAction, action)
                .eq(AuditLog::getResult, "SUCCESS"));
        assertThat(audits).as("审计动作 %s 应记录 SUCCESS", action).isNotEmpty();
    }
}

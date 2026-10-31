package com.taiji.fund.governance;

import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 权限矩阵：四类角色 × 关键接口，允许/拒绝与数据范围。
 */
class PermissionMatrixTest extends AbstractGovernanceTest {

    @Test
    @DisplayName("UT-G13 四角色关键接口权限矩阵")
    void ut13EndpointMatrix() throws Exception {
        String admin = adminToken();
        String pmo = pmoToken();
        String groupUser = loginToken("user01", "User@12345");
        String expert = loginToken("expert01", "Expert@12345");

        perform(get("/api/v1/users"), admin, 200);
        perform(get("/api/v1/audit-logs"), admin, 200);
        perform(get("/api/v1/system/configs"), admin, 200);
        perform(get("/api/v1/roles"), admin, 200);
        perform(get("/api/v1/groups/all"), admin, 200);

        perform(get("/api/v1/users"), pmo, 403);
        perform(get("/api/v1/audit-logs"), pmo, 403);
        perform(get("/api/v1/system/configs"), pmo, 403);
        perform(get("/api/v1/analysis/runs"), pmo, 200);
        perform(get("/api/v1/insight/trend"), pmo, 200);
        perform(get("/api/v1/decision/records"), pmo, 200);

        perform(get("/api/v1/users"), groupUser, 403);
        perform(get("/api/v1/analysis/runs"), groupUser, 403);
        perform(get("/api/v1/system/configs"), groupUser, 403);
        perform(get("/api/v1/projects?page=1&size=5"), groupUser, 200);
        perform(get("/api/v1/insight/trend"), groupUser, 200);
        perform(get("/api/v1/insight/investment/overview"), groupUser, 200);
        perform(get("/api/v1/decision/records"), groupUser, 200);

        perform(get("/api/v1/projects?page=1&size=5"), expert, 200);
        perform(get("/api/v1/tasks/todo"), expert, 200);
        perform(get("/api/v1/decision/records"), expert, 200);
        perform(get("/api/v1/insight/trend"), expert, 403);
        perform(get("/api/v1/insight/investment/overview"), expert, 403);
        perform(get("/api/v1/analysis/runs"), expert, 403);
        perform(get("/api/v1/users"), expert, 403);
        perform(post("/api/v1/decision/precheck"), expert, 403,
                "{\"mode\":\"TEXT\",\"goalText\":\"测试\"}");
        perform(post("/api/v1/decision/records"), expert, 403,
                "{\"riskScore\":80,\"level\":\"HIGH\",\"recommendation\":\"MERGE\",\"similarProjects\":\"[]\"}");
    }

    @Test
    @DisplayName("UT-G14 组用户不可访问他组项目，专家可访问 REVIEWING")
    void ut14GroupScopeAndExpertReviewing() throws Exception {
        String admin = adminToken();
        String groupUser = loginToken("user01", "User@12345");

        Long otherGroup = groupId("GRP-DATA");
        User otherOwner = createGroupUser("gov-scope-other", otherGroup);
        User otherMember = createGroupUser("gov-scope-other-m", otherGroup);
        long otherProject = createSubmittedProject(tokenOf(otherOwner), otherGroup,
                otherOwner.getId(), otherMember.getId(), uniqueProjectName("gov-scope-other"));

        perform(get("/api/v1/projects/" + otherProject), groupUser, 403);
        perform(post("/api/v1/decision/precheck"), groupUser, 403,
                "{\"mode\":\"PROJECT\",\"projectId\":" + otherProject + "}");

        String pmo = pmoToken();
        acceptProject(pmo, otherProject);
        User expert = createExpert("gov-scope-expert");
        String expertToken = tokenOf(expert);
        perform(get("/api/v1/projects/" + otherProject), expertToken, 200);
        perform(get("/api/v1/projects?page=1&size=5"), expertToken, 200);
        perform(get("/api/v1/system/configs"), expertToken, 403);
        perform(get("/api/v1/audit-logs"), expertToken, 403);

        long pmoDraft = createDraftId(tokenOf(otherOwner), uniqueProjectName("gov-pmo-scope"),
                otherGroup, otherOwner.getId());
        perform(post("/api/v1/projects/{id}/actions", pmoDraft), pmo, 403,
                "{\"action\":\"SUBMIT\"}");
        perform(post("/api/v1/projects/{id}/actions", pmoDraft), pmo, 403,
                "{\"action\":\"START\"}");
        perform(post("/api/v1/projects"), pmo, 403,
                projectJson(uniqueProjectName("gov-pmo-create"), otherGroup, otherOwner.getId(),
                        "1000000.00", "2026-11-01", "2027-10-31", null));
    }
}

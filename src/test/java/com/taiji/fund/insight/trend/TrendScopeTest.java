package com.taiji.fund.insight.trend;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-15/16：GROUP_USER 强制本组、EXPERT 403、未登录 401。
 */
class TrendScopeTest extends AbstractTrendTest {

    @Test
    void ut15GroupUserIsForcedToOwnGroup() throws Exception {
        Long intel = intelGroupId();
        Long data = dataGroupId();
        Long projectA = newProject(intel, "范围项目A");
        Long projectB = newProject(data, "范围项目B");
        AnalysisRun run = newRun();
        snapshot(run.getId(), projectA, intel, LocalDate.of(2022, 4, 10), "50.00", "40.00", 0, 0, 0, 0, 0);
        snapshot(run.getId(), projectB, data, LocalDate.of(2022, 4, 11), "90.00", "80.00", 1, 0, 0, 0, 0);

        User groupUser = createGroupUser("trendgroup", intel);
        String token = tokenOf(groupUser);

        JsonNode data1 = trend(token, "startMonth=2022-04&endMonth=2022-04&groupId=" + data);
        JsonNode point = lastPoint(data1);
        assertEquals(1, point.path("analyzedProjectCount").asInt());
        assertEquals(50.0, point.path("avgMaxScore").asDouble(), 0.001);

        JsonNode attribution = attribution(token, "startMonth=2022-04&endMonth=2022-04");
        assertEquals(1, attribution.path("groups").size());
        assertEquals(intel.longValue(), attribution.path("groups").get(0).path("groupId").asLong());
    }

    @Test
    void ut16ExpertForbiddenAndAnonymousUnauthorized() throws Exception {
        Long intel = intelGroupId();
        User expert = createUser("trendexp", PASSWORD, UserStatus.ENABLED, intel, RoleCode.EXPERT, 0);
        String token = loginToken(expert.getUsername(), PASSWORD);

        mockMvc.perform(get("/api/v1/insight/trend").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/insight/trend"))
                .andExpect(status().isUnauthorized());
    }
}

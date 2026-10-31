package com.taiji.fund.insight.investment;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UT-I10：GROUP_USER 强制本组、EXPERT 403、未登录 401。
 */
class InvestmentScopeTest extends AbstractInvestmentTest {

    @Test
    void ut10GroupUserForcedToOwnGroup() throws Exception {
        Group ownGroup = createGroup("投入本组");
        Group otherGroup = createGroup("投入他组");
        insertProject(ownGroup.getId(), "本组项目", "APPROVED", "PLATFORM",
                LocalDate.of(2037, 1, 1), "600000");
        insertProject(otherGroup.getId(), "他组项目", "APPROVED", "PLATFORM",
                LocalDate.of(2037, 2, 1), "900000");

        User user = createGroupUser("investmentgroup", ownGroup.getId());
        String token = tokenOf(user);

        JsonNode data = overview(token, "startYear=2037&endYear=2037&groupId=" + otherGroup.getId());
        assertEquals(1, data.path("projectCount").asInt());
        assertEquals(0, data.path("totalBudget").decimalValue()
                .compareTo(new java.math.BigDecimal("600000.00")));
    }

    @Test
    void ut10bExpertForbiddenAndAnonymousUnauthorized() throws Exception {
        User expert = createUser("investmentexp", PASSWORD, UserStatus.ENABLED, null, RoleCode.EXPERT, 0);
        String token = loginToken(expert.getUsername(), PASSWORD);

        mockMvc.perform(get("/api/v1/insight/investment/overview")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/insight/investment/overview"))
                .andExpect(status().isUnauthorized());
    }
}

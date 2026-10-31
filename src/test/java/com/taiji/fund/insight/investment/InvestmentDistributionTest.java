package com.taiji.fund.insight.investment;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT-I04/I06/I07：年份口径、同比、基金类型与技术方向分布。
 */
class InvestmentDistributionTest extends AbstractInvestmentTest {

    @Test
    void ut04YearUsesStartDateWithCreatedAtFallback() throws Exception {
        Long group = createGroup("年份口径组").getId();
        insertProject(group, "按开始日期归年", "APPROVED", "PLATFORM",
                LocalDate.of(2034, 1, 1), "600000");
        insertProject(group, "无开始日期回退创建时间", "APPROVED", "PLATFORM",
                null, "400000");

        JsonNode items = investmentList(adminToken(), "by-year", "groupId=" + group);
        assertEquals(2, items.size());
        int currentYear = LocalDate.now().getYear();
        boolean hasStartYear = false;
        boolean hasCreatedYear = false;
        for (JsonNode item : items) {
            if (String.valueOf(2034).equals(item.path("key").asText())) {
                hasStartYear = true;
                assertEquals(600000, item.path("budget").asInt());
            }
            if (String.valueOf(currentYear).equals(item.path("key").asText())) {
                hasCreatedYear = true;
                assertEquals(400000, item.path("budget").asInt());
            }
        }
        assertTrue(hasStartYear && hasCreatedYear);
    }

    @Test
    void ut06YoyBudgetAndCount() throws Exception {
        Long group = createGroup("同比口径组").getId();
        insertProject(group, "2034项目一", "APPROVED", "PLATFORM", LocalDate.of(2034, 1, 1), "600000");
        insertProject(group, "2034项目二", "IN_PROGRESS", "PLATFORM", LocalDate.of(2034, 2, 1), "400000");
        insertProject(group, "2035项目一", "APPROVED", "PLATFORM", LocalDate.of(2035, 1, 1), "600000");
        insertProject(group, "2035项目二", "IN_PROGRESS", "PLATFORM", LocalDate.of(2035, 2, 1), "500000");
        insertProject(group, "2035项目三", "APPROVED", "PLATFORM", LocalDate.of(2035, 3, 1), "400000");

        JsonNode data = overview(adminToken(), "groupId=" + group);
        assertEquals(0.5, data.path("yoyBudgetRatio").asDouble(), 0.0001);
        assertEquals(0.5, data.path("yoyCountRatio").asDouble(), 0.0001);
    }

    @Test
    void ut07FundTypeAndDirectionDistribution() throws Exception {
        Long group = createGroup("分布口径组").getId();
        insertProject(group, "平台一", "APPROVED", "PLATFORM", LocalDate.of(2036, 1, 1), "600000");
        insertProject(group, "平台二", "IN_PROGRESS", "PLATFORM", LocalDate.of(2036, 2, 1), "400000");
        insertProject(group, "创新一", "APPROVED", "INNOVATION", LocalDate.of(2036, 3, 1), "500000");

        JsonNode funds = investmentList(adminToken(), "by-fund-type", "groupId=" + group);
        assertEquals(2, funds.size(), "平台基金与创新基金两行");
        BigDecimal shareSum = BigDecimal.ZERO;
        for (JsonNode item : funds) {
            shareSum = shareSum.add(item.path("budgetShare").decimalValue());
            if ("PLATFORM".equals(item.path("key").asText())) {
                assertEquals(1000000, item.path("budget").asInt());
            }
        }
        assertEquals(0, shareSum.compareTo(BigDecimal.ONE));

        JsonNode directions = investmentList(adminToken(), "by-direction", "groupId=" + group);
        assertEquals(1, directions.size());
        assertEquals(1500000, directions.get(0).path("budget").asInt());
    }
}

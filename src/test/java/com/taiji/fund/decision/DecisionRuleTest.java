package com.taiji.fund.decision;

import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.decision.dto.DecisionRuleItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UT-D01/D02：默认规则边界、条件组合、规则保存与校验。
 */
class DecisionRuleTest extends AbstractDecisionTest {

    @Test
    @DisplayName("UT-D01 默认规则四档与 60/80/90 边界")
    void ut01DefaultRuleBoundaries() {
        assertThat(match("59.99", null, false).getAction()).isEqualTo("APPROVE");
        assertThat(match("60.00", null, false).getAction()).isEqualTo("EXPLAIN");
        assertThat(match("79.99", null, false).getAction()).isEqualTo("EXPLAIN");
        assertThat(match("80.00", null, true).getAction()).isEqualTo("MERGE");
        assertThat(match("80.00", "PEER_EARLIER", false).getAction()).isEqualTo("MERGE");
        assertThat(match("80.00", "OUR_EARLIER", false).getAction()).isEqualTo("EXPLAIN");
        assertThat(match("89.99", "SELF_GROUP", true).getAction()).isEqualTo("MERGE");
        assertThat(match("90.00", null, false).getAction()).isEqualTo("TERMINATE");
        assertThat(match("100.00", "SELF_GROUP", true).getAction()).isEqualTo("TERMINATE");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("UT-D02 规则保存后立即生效，非法规则被拒绝")
    void ut02SaveAndValidateRules() {
        List<DecisionRuleItem> items = new ArrayList<>();
        items.add(item(10, "0", "100", "EXPLAIN", "全区间补充差异说明"));
        items.add(item(20, "0", "100", "APPROVE", "兜底可立项"));

        List<DecisionRule> saved = decisionRuleService.save(items);
        assertThat(saved).hasSize(2);
        assertThat(match("10.00", null, false).getAction()).isEqualTo("EXPLAIN");

        assertThatThrownBy(() -> decisionRuleService.save(single(10, "60", "BAD_ACTION")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("建议动作");

        assertThatThrownBy(() -> decisionRuleService.save(single(10, "60", "APPROVE")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("分数区间");

        assertThatThrownBy(() -> decisionRuleService.save(single(10, "120", "APPROVE")))
                .isInstanceOf(BizException.class);
    }

    private DecisionRule match(String score, String relation, boolean sameGroup) {
        return decisionRuleService.match(new BigDecimal(score), relation, sameGroup);
    }

    private DecisionRuleItem item(int priority, String min, String max, String action, String description) {
        DecisionRuleItem item = new DecisionRuleItem();
        item.setPriority(priority);
        item.setMinScore(new BigDecimal(min));
        item.setMaxScore(new BigDecimal(max));
        item.setAction(action);
        item.setDescription(description);
        item.setEnabled(1);
        return item;
    }

    private List<DecisionRuleItem> single(int priority, String max, String action) {
        List<DecisionRuleItem> items = new ArrayList<>();
        items.add(item(priority, "0", max, action, "测试规则"));
        return items;
    }
}

package com.taiji.fund.decision;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.decision.dto.DecisionRuleItem;
import com.taiji.fund.decision.mapper.DecisionRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 决策规则：加载、匹配、整体替换与审计。
 */
@Service
@RequiredArgsConstructor
public class DecisionRuleService {

    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            DecisionConstants.ACTION_APPROVE,
            DecisionConstants.ACTION_EXPLAIN,
            DecisionConstants.ACTION_MERGE,
            DecisionConstants.ACTION_TERMINATE));

    private final DecisionRuleMapper ruleMapper;

    @Transactional(readOnly = true)
    public List<DecisionRule> list() {
        return ruleMapper.selectList(Wrappers.<DecisionRule>lambdaQuery()
                .orderByAsc(DecisionRule::getPriority)
                .orderByAsc(DecisionRule::getId));
    }

    /**
     * 按优先级匹配首个启用规则；未命中回退 EXPLAIN。
     */
    @Transactional(readOnly = true)
    public DecisionRule match(BigDecimal score, String relation, boolean sameGroup) {
        BigDecimal value = score == null ? BigDecimal.ZERO : score;
        for (DecisionRule rule : list()) {
            if (!Integer.valueOf(1).equals(rule.getEnabled())) {
                continue;
            }
            if (value.compareTo(rule.getMinScore()) < 0) {
                continue;
            }
            boolean upperMatched = rule.getMaxScore() != null
                    && (value.compareTo(rule.getMaxScore()) < 0
                    || rule.getMaxScore().compareTo(BigDecimal.valueOf(100)) >= 0);
            if (!upperMatched) {
                continue;
            }
            if (StringUtils.hasText(rule.getRelation())
                    && !rule.getRelation().equals(relation)) {
                continue;
            }
            if (rule.getSameGroup() != null
                    && (rule.getSameGroup() == 1) != sameGroup) {
                continue;
            }
            return rule;
        }
        return fallback();
    }

    public DecisionRule fallback() {
        DecisionRule rule = new DecisionRule();
        rule.setPriority(Integer.MAX_VALUE);
        rule.setMinScore(BigDecimal.ZERO);
        rule.setMaxScore(BigDecimal.valueOf(100));
        rule.setAction(DecisionConstants.ACTION_EXPLAIN);
        rule.setDescription("未命中任何规则，回退人工判断");
        rule.setEnabled(1);
        return rule;
    }

    @Audit(action = "DECISION_RULE_UPDATE", resourceType = "DECISION_RULE")
    @Transactional
    public List<DecisionRule> save(List<DecisionRuleItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.DECISION_RULE_INVALID, "规则列表不能为空");
        }
        List<DecisionRule> rules = new ArrayList<>(items.size());
        boolean coversLow = false;
        boolean coversHigh = false;
        for (DecisionRuleItem item : items) {
            DecisionRule rule = validate(item);
            rules.add(rule);
            if (rule.getMinScore().compareTo(BigDecimal.ZERO) <= 0) {
                coversLow = true;
            }
            if (rule.getMaxScore().compareTo(BigDecimal.valueOf(100)) >= 0) {
                coversHigh = true;
            }
        }
        if (!coversLow || !coversHigh) {
            throw new BizException(ErrorCode.DECISION_RULE_INVALID,
                    "规则必须覆盖 0 到 100 的完整分数区间");
        }
        rules.sort(Comparator.comparing(DecisionRule::getPriority)
                .thenComparing(rule -> rule.getId() == null ? Long.MAX_VALUE : rule.getId()));
        ruleMapper.delete(Wrappers.<DecisionRule>lambdaQuery());
        for (DecisionRule rule : rules) {
            ruleMapper.insert(rule);
        }
        return list();
    }

    private DecisionRule validate(DecisionRuleItem item) {
        if (item == null || item.getPriority() == null || item.getPriority() <= 0) {
            throw new BizException(ErrorCode.DECISION_RULE_INVALID, "规则优先级必须为正整数");
        }
        if (item.getMinScore() == null || item.getMaxScore() == null
                || item.getMinScore().compareTo(BigDecimal.ZERO) < 0
                || item.getMaxScore().compareTo(BigDecimal.valueOf(100)) > 0
                || item.getMinScore().compareTo(item.getMaxScore()) >= 0) {
            throw new BizException(ErrorCode.DECISION_RULE_INVALID,
                    "分数区间必须满足 0 ≤ min < max ≤ 100");
        }
        String action = item.getAction() == null
                ? "" : item.getAction().trim().toUpperCase(Locale.ROOT);
        if (!ACTIONS.contains(action)) {
            throw new BizException(ErrorCode.DECISION_RULE_INVALID,
                    "建议动作必须为 APPROVE/EXPLAIN/MERGE/TERMINATE");
        }
        if (item.getSameGroup() != null
                && item.getSameGroup() != 0 && item.getSameGroup() != 1) {
            throw new BizException(ErrorCode.DECISION_RULE_INVALID, "同组条件只能为 0/1 或空");
        }
        DecisionRule rule = new DecisionRule();
        rule.setPriority(item.getPriority());
        rule.setMinScore(item.getMinScore());
        rule.setMaxScore(item.getMaxScore());
        rule.setRelation(StringUtils.hasText(item.getRelation())
                ? item.getRelation().trim().toUpperCase(Locale.ROOT) : null);
        rule.setSameGroup(item.getSameGroup());
        rule.setAction(action);
        rule.setDescription(item.getDescription());
        rule.setEnabled(item.getEnabled() == null ? 1 : item.getEnabled());
        rule.setCreatedAt(java.time.LocalDateTime.now());
        rule.setUpdatedAt(java.time.LocalDateTime.now());
        return rule;
    }
}

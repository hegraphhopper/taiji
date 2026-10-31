package com.taiji.fund.decision;

import com.taiji.fund.analysis.result.dto.AnalysisPrecheckRequest;
import com.taiji.fund.analysis.result.dto.AnalysisPrecheckVO;
import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import com.taiji.fund.analysis.run.AnalysisRunService;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.decision.dto.DecisionPrecheckRequest;
import com.taiji.fund.decision.dto.DecisionPrecheckVO;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.service.ProjectAccessService;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

/**
 * 辅助决策预检：复用分析引擎的预检能力，叠加规则、金额与专家建议。
 */
@Service
@RequiredArgsConstructor
public class DecisionPrecheckService {

    private static final String REASON_PREFIX = "本地算法结果（大模型未参与）：";

    private final AnalysisRunService analysisRunService;
    private final DecisionRuleService ruleService;
    private final DecisionExpertService expertService;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService accessService;
    private final GroupMapper groupMapper;
    private final TechDirectionMapper techDirectionMapper;

    public DecisionPrecheckVO precheck(DecisionPrecheckRequest request) {
        String mode = resolveMode(request);
        Project project = null;
        AnalysisPrecheckRequest analysisRequest = new AnalysisPrecheckRequest();
        if (DecisionConstants.MODE_PROJECT.equals(mode)) {
            if (request.getProjectId() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "PROJECT 模式必须选择项目");
            }
            project = projectMapper.selectById(request.getProjectId());
            if (project == null) {
                throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
            }
            accessService.assertCanView(project);
            analysisRequest.setProjectId(project.getId());
        } else {
            if (!hasText(request.getGoalText()) && !hasText(request.getContentText())
                    && !hasText(request.getTechText()) && !hasText(request.getMilestoneText())) {
                throw new BizException(ErrorCode.ANALYSIS_TEXT_REQUIRED);
            }
            analysisRequest.setGoalText(request.getGoalText());
            analysisRequest.setContentText(request.getContentText());
            analysisRequest.setTechText(request.getTechText());
            analysisRequest.setMilestoneText(request.getMilestoneText());
            analysisRequest.setTechDirectionId(request.getTechDirectionId());
        }

        AnalysisPrecheckVO outcome = analysisRunService.precheck(analysisRequest);
        List<PeerMatchVO> topPeers = outcome.getTopPeers();
        PeerMatchVO top = topPeers == null || topPeers.isEmpty() ? null : topPeers.get(0);

        BigDecimal riskScore = top == null || top.getTotalScore() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : top.getTotalScore();
        String level = top == null || top.getLevel() == null
                ? DecisionConstants.LEVEL_NONE : top.getLevel();
        String relation = top == null ? null : top.getRelation();
        boolean sameGroup = top != null && "SELF_GROUP".equals(top.getRelation());

        DecisionRule rule = ruleService.match(riskScore, relation, sameGroup);

        BigDecimal budget = project == null ? request.getBudget() : project.getBudget();
        BigDecimal savings = budget == null ? null : budget.multiply(riskScore)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Long directionId = project == null ? request.getTechDirectionId() : project.getTechDirectionId();
        String directionName = directionName(directionId);
        String groupName = project == null || project.getGroupId() == null
                ? null : groupName(project.getGroupId());

        return DecisionPrecheckVO.builder()
                .mode(mode)
                .runId(outcome.getRunId())
                .projectId(project == null ? null : project.getId())
                .projectCode(project == null ? null : project.getCode())
                .projectName(project == null ? null : project.getName())
                .groupName(groupName)
                .techDirectionName(directionName)
                .budget(budget)
                .riskScore(riskScore)
                .level(level)
                .topPeers(topPeers)
                .recommendation(rule.getAction())
                .ruleId(rule.getId())
                .ruleDescription(rule.getDescription())
                .reason(reason(riskScore, level, top, rule))
                .savingsEstimate(savings)
                .suggestedExperts(expertService.suggest(directionId))
                .llmUsed(false)
                .inputHash(project == null ? hash(request) : null)
                .build();
    }

    private String resolveMode(DecisionPrecheckRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "预检请求不能为空");
        }
        if (StringUtils.hasText(request.getMode())) {
            String mode = request.getMode().trim().toUpperCase(Locale.ROOT);
            if (DecisionConstants.MODE_PROJECT.equals(mode)
                    || DecisionConstants.MODE_TEXT.equals(mode)) {
                return mode;
            }
            throw new BizException(ErrorCode.BAD_REQUEST, "预检模式必须为 PROJECT/TEXT");
        }
        return request.getProjectId() == null
                ? DecisionConstants.MODE_TEXT : DecisionConstants.MODE_PROJECT;
    }

    private String reason(BigDecimal score, String level, PeerMatchVO top, DecisionRule rule) {
        StringBuilder builder = new StringBuilder(REASON_PREFIX);
        builder.append("最高重复度 ").append(score.toPlainString())
                .append("（").append(level).append("）");
        if (top != null && top.getPeer() != null) {
            builder.append("，最相似项目《").append(top.getPeer().getName()).append("》");
            if (top.getDirection() != null) {
                builder.append("，归因「").append(directionLabel(top.getDirection())).append("」");
            }
        } else {
            builder.append("，未发现可比对项目");
        }
        builder.append("；命中规则「").append(rule.getDescription()).append("」")
                .append("，建议动作 ").append(rule.getAction()).append("。");
        if (DecisionConstants.ACTION_EXPLAIN.equals(rule.getAction())) {
            builder.append("建议补充与相似项目的差异说明后再提交。");
        } else if (DecisionConstants.ACTION_MERGE.equals(rule.getAction())) {
            builder.append("建议与相似项目合并或明确分工后立项。");
        } else if (DecisionConstants.ACTION_TERMINATE.equals(rule.getAction())) {
            builder.append("核心内容高度重合，建议终止或调整技术路线。");
        }
        return builder.toString();
    }

    private String directionLabel(String direction) {
        if ("SELF_GROUP".equals(direction)) {
            return "本组内部重复";
        }
        if ("PEER_EARLIER".equals(direction)) {
            return "他组在先";
        }
        if ("OUR_EARLIER".equals(direction)) {
            return "我方在先";
        }
        if ("SIMULTANEOUS".equals(direction)) {
            return "同期并行";
        }
        return direction;
    }

    private String groupName(Long groupId) {
        Group group = groupMapper.selectById(groupId);
        return group == null ? null : group.getName();
    }

    private String directionName(Long directionId) {
        if (directionId == null) {
            return null;
        }
        TechDirection direction = techDirectionMapper.selectById(directionId);
        return direction == null ? null : direction.getName();
    }

    private String hash(DecisionPrecheckRequest request) {
        String raw = nullSafe(request.getGoalText()) + '\u0000'
                + nullSafe(request.getContentText()) + '\u0000'
                + nullSafe(request.getTechText()) + '\u0000'
                + nullSafe(request.getMilestoneText());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

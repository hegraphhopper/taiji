package com.taiji.fund.decision;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiji.fund.analysis.result.dto.PeerMatchVO;
import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.decision.dto.DecisionRecordQuery;
import com.taiji.fund.decision.dto.DecisionRecordVO;
import com.taiji.fund.decision.dto.DecisionSaveRequest;
import com.taiji.fund.decision.mapper.DecisionRecordMapper;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.service.ProjectAccessService;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 决策记录：保存预检快照、按数据范围查询、作废（软删除）。
 */
@Service
@RequiredArgsConstructor
public class DecisionRecordService {

    private static final long MAX_PAGE_SIZE = 100L;
    private static final int MAX_SNAPSHOT_LENGTH = 60_000;
    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            DecisionConstants.ACTION_APPROVE,
            DecisionConstants.ACTION_EXPLAIN,
            DecisionConstants.ACTION_MERGE,
            DecisionConstants.ACTION_TERMINATE));

    private final DecisionRecordMapper recordMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService accessService;
    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final ObjectMapper objectMapper;

    @Audit(action = "DECISION_RECORD_CREATE", resourceType = "DECISION_RECORD")
    @Transactional
    public DecisionRecordVO save(DecisionSaveRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "决策记录不能为空");
        }
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectMapper.selectById(request.getProjectId());
            if (project == null) {
                throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
            }
            accessService.assertCanView(project);
        }
        if (request.getLevel() == null || request.getLevel().trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "风险等级不能为空");
        }
        String action = request.getRecommendation() == null
                ? "" : request.getRecommendation().trim().toUpperCase();
        if (!ACTIONS.contains(action)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "建议动作必须为 APPROVE/EXPLAIN/MERGE/TERMINATE");
        }
        String snapshot = normalizeSnapshot(request.getSimilarProjects());

        DecisionRecord record = new DecisionRecord();
        record.setProjectId(request.getProjectId());
        record.setRunId(request.getRunId());
        record.setRiskScore(request.getRiskScore() == null
                ? java.math.BigDecimal.ZERO : request.getRiskScore());
        record.setLevel(request.getLevel().trim().toUpperCase());
        record.setRecommendation(action);
        record.setReason(request.getReason());
        record.setSimilarProjects(snapshot);
        record.setSavingsEstimate(request.getSavingsEstimate());
        record.setInputHash(request.getInputHash());
        record.setOperatorId(SecurityUtils.currentUserId());
        record.setCreatedAt(LocalDateTime.now());
        record.setDeleted(0);
        recordMapper.insert(record);
        return detail(record.getId());
    }

    @Transactional(readOnly = true)
    public PageResult<DecisionRecordVO> list(DecisionRecordQuery query) {
        DecisionRecordQuery actual = query == null ? new DecisionRecordQuery() : query;
        List<DecisionRecord> records = recordMapper.selectList(Wrappers.<DecisionRecord>lambdaQuery()
                .eq(actual.getProjectId() != null, DecisionRecord::getProjectId, actual.getProjectId())
                .eq(StringUtils.hasText(actual.getLevel()), DecisionRecord::getLevel,
                        actual.getLevel() == null ? null : actual.getLevel().trim().toUpperCase())
                .eq(StringUtils.hasText(actual.getRecommendation()),
                        DecisionRecord::getRecommendation,
                        actual.getRecommendation() == null
                                ? null : actual.getRecommendation().trim().toUpperCase())
                .ge(actual.getFrom() != null, DecisionRecord::getCreatedAt,
                        actual.getFrom() == null ? null : actual.getFrom().atStartOfDay())
                .le(actual.getTo() != null, DecisionRecord::getCreatedAt,
                        actual.getTo() == null ? null : actual.getTo().atTime(23, 59, 59))
                .orderByDesc(DecisionRecord::getCreatedAt)
                .orderByDesc(DecisionRecord::getId));

        Map<Long, Project> projects = loadProjects(records);
        List<DecisionRecord> visible = new ArrayList<>();
        for (DecisionRecord record : records) {
            if (matchesGroup(record, projects, actual.getGroupId())
                    && canView(record, projects)) {
                visible.add(record);
            }
        }
        long pageNo = Math.max(1L, actual.getPage() == null ? 1L : actual.getPage());
        long pageSize = Math.min(MAX_PAGE_SIZE,
                Math.max(1L, actual.getSize() == null ? 10L : actual.getSize()));
        int fromIndex = (int) Math.min((pageNo - 1) * pageSize, visible.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, visible.size());
        List<DecisionRecordVO> result = visible.subList(fromIndex, toIndex).stream()
                .map(record -> toVO(record, projects.get(record.getProjectId())))
                .collect(Collectors.toList());
        return PageResult.of(result, visible.size(), pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public DecisionRecordVO detail(Long id) {
        DecisionRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ErrorCode.DECISION_RECORD_NOT_FOUND);
        }
        Project project = record.getProjectId() == null
                ? null : projectMapper.selectById(record.getProjectId());
        if (!canView(record, project == null
                ? Collections.emptyMap() : Collections.singletonMap(project.getId(), project))) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该决策记录");
        }
        return toVO(record, project);
    }

    @Audit(action = "DECISION_RECORD_VOID", resourceType = "DECISION_RECORD")
    @Transactional
    public void voidRecord(Long id) {
        DecisionRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ErrorCode.DECISION_RECORD_NOT_FOUND);
        }
        recordMapper.deleteById(id);
    }

    private boolean matchesGroup(DecisionRecord record, Map<Long, Project> projects, Long groupId) {
        if (groupId == null) {
            return true;
        }
        Project project = record.getProjectId() == null ? null : projects.get(record.getProjectId());
        return project != null && groupId.equals(project.getGroupId());
    }

    private boolean canView(DecisionRecord record, Map<Long, Project> projects) {
        LoginUser user = SecurityUtils.currentUserOrNull();
        if (user == null) {
            return false;
        }
        if (user.getRoles().contains(RoleCode.ADMIN.name())
                || user.getRoles().contains(RoleCode.PMO.name())) {
            return true;
        }
        if (user.getRoles().contains(RoleCode.GROUP_USER.name())) {
            if (record.getProjectId() == null) {
                return user.getId() != null && user.getId().equals(record.getOperatorId());
            }
            Project project = projects.get(record.getProjectId());
            return project != null && user.getGroupId() != null
                    && user.getGroupId().equals(project.getGroupId());
        }
        if (user.getRoles().contains(RoleCode.EXPERT.name())) {
            Project project = record.getProjectId() == null
                    ? null : projects.get(record.getProjectId());
            return project != null && user.getGroupId() != null
                    && user.getGroupId().equals(project.getGroupId())
                    && !ProjectStatus.DRAFT.getCode().equals(project.getStatus());
        }
        return false;
    }

    private Map<Long, Project> loadProjects(List<DecisionRecord> records) {
        List<Long> ids = records.stream().map(DecisionRecord::getProjectId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return projectMapper.selectList(Wrappers.<Project>lambdaQuery().in(Project::getId, ids))
                .stream().collect(Collectors.toMap(Project::getId, project -> project, (l, r) -> l));
    }

    private DecisionRecordVO toVO(DecisionRecord record, Project project) {
        return DecisionRecordVO.builder()
                .id(record.getId())
                .projectId(record.getProjectId())
                .projectCode(project == null ? null : project.getCode())
                .projectName(project == null ? null : project.getName())
                .groupName(project == null ? null : groupName(project.getGroupId()))
                .runId(record.getRunId())
                .riskScore(record.getRiskScore())
                .level(record.getLevel())
                .recommendation(record.getRecommendation())
                .reason(record.getReason())
                .similarProjects(parseSnapshot(record.getSimilarProjects()))
                .savingsEstimate(record.getSavingsEstimate())
                .inputHash(record.getInputHash())
                .operatorId(record.getOperatorId())
                .operatorName(operatorName(record.getOperatorId()))
                .createdAt(record.getCreatedAt())
                .build();
    }

    private String groupName(Long groupId) {
        if (groupId == null) {
            return null;
        }
        Group group = groupMapper.selectById(groupId);
        return group == null ? null : group.getName();
    }

    private String operatorName(Long operatorId) {
        if (operatorId == null) {
            return null;
        }
        User user = userMapper.selectById(operatorId);
        if (user == null) {
            return null;
        }
        return user.getRealName() == null ? user.getUsername() : user.getRealName();
    }

    private String normalizeSnapshot(String json) {
        if (!StringUtils.hasText(json)) {
            return "[]";
        }
        if (json.length() > MAX_SNAPSHOT_LENGTH) {
            throw new BizException(ErrorCode.BAD_REQUEST, "相似项目快照过大");
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "相似项目快照不是合法 JSON");
        }
        return json;
    }

    private List<PeerMatchVO> parseSnapshot(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PeerMatchVO>>() {
            });
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }
}

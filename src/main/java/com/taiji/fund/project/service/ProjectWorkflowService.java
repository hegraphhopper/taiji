package com.taiji.fund.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.ProjectAction;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectApprovalLog;
import com.taiji.fund.project.dto.ProjectActionRequest;
import com.taiji.fund.project.dto.ProjectApprovalLogVO;
import com.taiji.fund.project.dto.ProjectDetailVO;
import com.taiji.fund.project.mapper.ProjectApprovalLogMapper;
import com.taiji.fund.project.mapper.ProjectContentMapper;
import com.taiji.fund.project.mapper.ProjectGoalMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectMilestoneMapper;
import com.taiji.fund.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectWorkflowService {

    private final ProjectMapper projectMapper;
    private final ProjectGoalMapper goalMapper;
    private final ProjectContentMapper contentMapper;
    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectApprovalLogMapper approvalLogMapper;
    private final ProjectAccessService accessService;
    private final ProjectQueryService queryService;

    @Transactional
    @Audit(action = "PROJECT_SUBMIT", resourceType = "PROJECT")
    public ProjectDetailVO submit(Long id, ProjectActionRequest request) {
        Project project = load(id);
        accessService.assertCanEdit(project);
        if (!ProjectStatus.DRAFT.getCode().equals(project.getStatus())
                && !ProjectStatus.REJECTED.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_ILLEGAL_TRANSITION, "仅草稿或已驳回项目可提交");
        }
        validateForSubmit(project);
        project.setSubmitTime(project.getSubmitTime() == null
                ? LocalDateTime.now() : project.getSubmitTime());
        transition(project, ProjectStatus.SUBMITTED.getCode(), ProjectAction.SUBMIT, null, request);
        return queryService.detail(id);
    }

    @Transactional
    @Audit(action = "PROJECT_WITHDRAW", resourceType = "PROJECT")
    public ProjectDetailVO withdraw(Long id, ProjectActionRequest request) {
        Project project = load(id);
        accessService.assertCanEdit(project);
        if (!ProjectStatus.SUBMITTED.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_ILLEGAL_TRANSITION, "仅已提交项目可撤回");
        }
        LoginUser user = accessService.currentUser();
        if (!user.getId().equals(project.getCreatedBy())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅提交人可撤回");
        }
        transition(project, ProjectStatus.DRAFT.getCode(), ProjectAction.WITHDRAW,
                trim(request.getComment()), request);
        return queryService.detail(id);
    }

    @Transactional
    @Audit(action = "PROJECT_APPROVE", resourceType = "PROJECT")
    public ProjectDetailVO approve(Long id, ProjectActionRequest request) {
        assertApprover();
        Project project = load(id);
        if (!ProjectStatus.SUBMITTED.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_ILLEGAL_TRANSITION, "仅已提交项目可确认立项");
        }
        transition(project, ProjectStatus.APPROVED.getCode(), ProjectAction.APPROVE,
                trim(request.getComment()), request);
        return queryService.detail(id);
    }

    @Transactional
    @Audit(action = "PROJECT_REJECT", resourceType = "PROJECT")
    public ProjectDetailVO reject(Long id, ProjectActionRequest request) {
        assertApprover();
        Project project = load(id);
        if (!ProjectStatus.SUBMITTED.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_ILLEGAL_TRANSITION, "仅已提交项目可驳回");
        }
        String comment = trim(request.getComment());
        if (comment == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "驳回必须填写意见");
        }
        transition(project, ProjectStatus.REJECTED.getCode(), ProjectAction.REJECT, comment, request);
        return queryService.detail(id);
    }

    @Transactional(readOnly = true)
    public List<ProjectApprovalLogVO> logs(Long id) {
        Project project = load(id);
        accessService.assertCanView(project);
        return approvalLogMapper.selectList(Wrappers.<ProjectApprovalLog>lambdaQuery()
                        .eq(ProjectApprovalLog::getProjectId, id)
                        .orderByAsc(ProjectApprovalLog::getId))
                .stream()
                .map(ProjectApprovalLogVO::from)
                .collect(java.util.stream.Collectors.toList());
    }

    private void transition(Project project, String toStatus, ProjectAction action,
                            String comment, ProjectActionRequest request) {
        String fromStatus = project.getStatus();
        project.setStatus(toStatus);
        if (request.getVersion() != null) {
            project.setVersion(request.getVersion());
        }
        int updated = projectMapper.updateById(project);
        if (updated == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目已被他人修改，请刷新后重试");
        }
        ProjectApprovalLog log = new ProjectApprovalLog();
        log.setProjectId(project.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action.getCode());
        log.setOperatorId(accessService.currentUserId());
        log.setComment(comment);
        log.setCreatedAt(LocalDateTime.now());
        approvalLogMapper.insert(log);
    }

    private void validateForSubmit(Project project) {
        if (project.getBudget() == null || project.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "预算必须大于 0");
        }
        if (project.getStartDate() == null || project.getEndDate() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请填写项目起止日期");
        }
        if (project.getEndDate().isBefore(project.getStartDate())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }
        if (goalMapper.selectCount(Wrappers.<com.taiji.fund.project.domain.ProjectGoal>lambdaQuery()
                .eq(com.taiji.fund.project.domain.ProjectGoal::getProjectId, project.getId())) == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "至少填写一条项目目标");
        }
        if (contentMapper.selectCount(Wrappers.<com.taiji.fund.project.domain.ProjectContent>lambdaQuery()
                .eq(com.taiji.fund.project.domain.ProjectContent::getProjectId, project.getId())) == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "至少填写一条研制内容");
        }
        if (milestoneMapper.selectCount(Wrappers
                .<com.taiji.fund.project.domain.ProjectMilestone>lambdaQuery()
                .eq(com.taiji.fund.project.domain.ProjectMilestone::getProjectId, project.getId())) == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "至少填写一条节点计划");
        }
    }

    private void assertApprover() {
        if (!isApprover(accessService.currentUser())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅基金办或管理员可执行该操作");
        }
    }

    private boolean isApprover(LoginUser user) {
        return user.getRoles().contains(RoleCode.ADMIN.name())
                || user.getRoles().contains(RoleCode.PMO.name());
    }

    private Project load(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

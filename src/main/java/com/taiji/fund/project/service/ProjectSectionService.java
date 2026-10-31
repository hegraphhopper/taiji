package com.taiji.fund.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.Difficulty;
import com.taiji.fund.common.enums.MemberRole;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectContent;
import com.taiji.fund.project.domain.ProjectGoal;
import com.taiji.fund.project.domain.ProjectKeyTech;
import com.taiji.fund.project.domain.ProjectMember;
import com.taiji.fund.project.domain.ProjectMilestone;
import com.taiji.fund.project.dto.SectionSaveRequest;
import com.taiji.fund.project.mapper.ProjectContentMapper;
import com.taiji.fund.project.mapper.ProjectGoalMapper;
import com.taiji.fund.project.mapper.ProjectKeyTechMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectMemberMapper;
import com.taiji.fund.project.mapper.ProjectMilestoneMapper;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectSectionService {

    private final ProjectMapper projectMapper;
    private final ProjectGoalMapper goalMapper;
    private final ProjectContentMapper contentMapper;
    private final ProjectKeyTechMapper keyTechMapper;
    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final ProjectAccessService accessService;

    @Transactional
    @Audit(action = "PROJECT_SECTIONS_SAVE", resourceType = "PROJECT")
    public void save(Long projectId, SectionSaveRequest request) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        accessService.assertCanEdit(project);
        if (!ProjectStatus.DRAFT.getCode().equals(project.getStatus())
                && !ProjectStatus.REJECTED.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_NOT_EDITABLE);
        }

        if (request.getGoals() != null) {
            replaceGoals(projectId, request.getGoals());
        }
        if (request.getContents() != null) {
            replaceContents(projectId, request.getContents());
        }
        if (request.getKeyTechs() != null) {
            replaceKeyTechs(projectId, request.getKeyTechs());
        }
        if (request.getMilestones() != null) {
            replaceMilestones(projectId, request.getMilestones());
        }
        if (request.getMembers() != null) {
            replaceMembers(projectId, request.getMembers());
        }
    }

    private void replaceGoals(Long projectId, List<SectionSaveRequest.GoalItem> items) {
        goalMapper.delete(Wrappers.<ProjectGoal>lambdaQuery().eq(ProjectGoal::getProjectId, projectId));
        for (int index = 0; index < items.size(); index++) {
            SectionSaveRequest.GoalItem item = items.get(index);
            ProjectGoal goal = new ProjectGoal();
            goal.setProjectId(projectId);
            goal.setContent(item.getContent().trim());
            goal.setSort(item.getSort() == null ? index : item.getSort());
            goalMapper.insert(goal);
        }
    }

    private void replaceContents(Long projectId, List<SectionSaveRequest.ContentItem> items) {
        contentMapper.delete(Wrappers.<ProjectContent>lambdaQuery().eq(ProjectContent::getProjectId, projectId));
        for (int index = 0; index < items.size(); index++) {
            SectionSaveRequest.ContentItem item = items.get(index);
            ProjectContent content = new ProjectContent();
            content.setProjectId(projectId);
            content.setContent(item.getContent().trim());
            content.setSort(item.getSort() == null ? index : item.getSort());
            contentMapper.insert(content);
        }
    }

    private void replaceKeyTechs(Long projectId, List<SectionSaveRequest.KeyTechItem> items) {
        keyTechMapper.delete(Wrappers.<ProjectKeyTech>lambdaQuery().eq(ProjectKeyTech::getProjectId, projectId));
        for (int index = 0; index < items.size(); index++) {
            SectionSaveRequest.KeyTechItem item = items.get(index);
            validateDifficulty(item.getDifficulty());
            ProjectKeyTech tech = new ProjectKeyTech();
            tech.setProjectId(projectId);
            tech.setName(item.getName().trim());
            tech.setDescription(item.getDescription());
            tech.setDifficulty(item.getDifficulty());
            tech.setSort(item.getSort() == null ? index : item.getSort());
            keyTechMapper.insert(tech);
        }
    }

    private void replaceMilestones(Long projectId, List<SectionSaveRequest.MilestoneItem> items) {
        milestoneMapper.delete(Wrappers.<ProjectMilestone>lambdaQuery()
                .eq(ProjectMilestone::getProjectId, projectId));
        for (int index = 0; index < items.size(); index++) {
            SectionSaveRequest.MilestoneItem item = items.get(index);
            if (item.getPlanStart() != null && item.getPlanEnd() != null
                    && item.getPlanEnd().isBefore(item.getPlanStart())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "节点结束日期不能早于开始日期");
            }
            ProjectMilestone milestone = new ProjectMilestone();
            milestone.setProjectId(projectId);
            milestone.setName(item.getName().trim());
            milestone.setPlanStart(item.getPlanStart());
            milestone.setPlanEnd(item.getPlanEnd());
            milestone.setDeliverable(item.getDeliverable());
            milestone.setWeight(item.getWeight() == null ? 0 : item.getWeight());
            milestone.setSort(item.getSort() == null ? index : item.getSort());
            milestoneMapper.insert(milestone);
        }
    }

    private void replaceMembers(Long projectId, List<SectionSaveRequest.MemberItem> items) {
        memberMapper.delete(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId));
        for (SectionSaveRequest.MemberItem item : items) {
            validateMemberRole(item.getMemberRole());
            if (userMapper.selectById(item.getUserId()) == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "项目成员用户不存在");
            }
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(item.getUserId());
            member.setMemberRole(item.getMemberRole());
            member.setWorkload(item.getWorkload());
            memberMapper.insert(member);
        }
    }

    private void validateDifficulty(String difficulty) {
        if (difficulty == null) {
            return;
        }
        boolean valid = Difficulty.HIGH.getCode().equals(difficulty)
                || Difficulty.MEDIUM.getCode().equals(difficulty)
                || Difficulty.LOW.getCode().equals(difficulty);
        if (!valid) {
            throw new BizException(ErrorCode.BAD_REQUEST, "关键技术难度不合法");
        }
    }

    private void validateMemberRole(String role) {
        boolean valid = MemberRole.LEADER.getCode().equals(role)
                || MemberRole.CORE.getCode().equals(role)
                || MemberRole.PARTICIPANT.getCode().equals(role);
        if (!valid) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目成员角色不合法");
        }
    }
}

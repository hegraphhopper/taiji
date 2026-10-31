package com.taiji.fund.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.enums.FundType;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.dto.ProjectApprovalLogVO;
import com.taiji.fund.project.dto.ProjectContentVO;
import com.taiji.fund.project.dto.ProjectDetailVO;
import com.taiji.fund.project.dto.ProjectDocumentVO;
import com.taiji.fund.project.dto.ProjectGoalVO;
import com.taiji.fund.project.dto.ProjectKeyTechVO;
import com.taiji.fund.project.dto.ProjectListItemVO;
import com.taiji.fund.project.dto.ProjectMemberVO;
import com.taiji.fund.project.dto.ProjectMilestoneVO;
import com.taiji.fund.project.dto.ProjectQuery;
import com.taiji.fund.project.dto.ProjectReviewVO;
import com.taiji.fund.project.mapper.ProjectApprovalLogMapper;
import com.taiji.fund.project.mapper.ProjectContentMapper;
import com.taiji.fund.project.mapper.ProjectDocumentMapper;
import com.taiji.fund.project.mapper.ProjectGoalMapper;
import com.taiji.fund.project.mapper.ProjectKeyTechMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectMemberMapper;
import com.taiji.fund.project.mapper.ProjectMilestoneMapper;
import com.taiji.fund.project.mapper.ProjectReviewMapper;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import com.taiji.fund.system.config.ReviewConfigService;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectQueryService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final ProjectMapper projectMapper;
    private final ProjectGoalMapper goalMapper;
    private final ProjectContentMapper contentMapper;
    private final ProjectKeyTechMapper keyTechMapper;
    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectApprovalLogMapper approvalLogMapper;
    private final ProjectDocumentMapper documentMapper;
    private final ProjectReviewMapper reviewMapper;
    private final ProjectReviewService reviewService;
    private final ReviewConfigService reviewConfigService;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final TechDirectionMapper techDirectionMapper;
    private final ProjectAccessService accessService;

    @Transactional(readOnly = true)
    public PageResult<ProjectListItemVO> list(ProjectQuery query) {
        LambdaQueryWrapper<Project> wrapper = Wrappers.lambdaQuery();
        applyScope(wrapper);

        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            List<Long> ownerIds = userMapper.selectList(Wrappers.<User>lambdaQuery()
                            .like(User::getRealName, keyword)
                            .or()
                            .like(User::getUsername, keyword))
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            wrapper.and(nested -> {
                nested.like(Project::getName, keyword).or().like(Project::getCode, keyword);
                if (!ownerIds.isEmpty()) {
                    nested.or().in(Project::getOwnerId, ownerIds);
                }
            });
        }
        if (query.getGroupId() != null) {
            accessService.assertGroupVisible(query.getGroupId());
            wrapper.eq(Project::getGroupId, query.getGroupId());
        }
        if (StringUtils.hasText(query.getFundType())) {
            validateFundType(query.getFundType());
            wrapper.eq(Project::getFundType, query.getFundType());
        }
        if (query.getTechDirectionId() != null) {
            wrapper.eq(Project::getTechDirectionId, query.getTechDirectionId());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            validateStatuses(query.getStatus());
            wrapper.in(Project::getStatus, query.getStatus());
        }
        if (query.getBudgetMin() != null) {
            wrapper.ge(Project::getBudget, query.getBudgetMin());
        }
        if (query.getBudgetMax() != null) {
            wrapper.le(Project::getBudget, query.getBudgetMax());
        }
        if (query.getDuplicateScoreMin() != null) {
            wrapper.ge(Project::getDuplicateScore, query.getDuplicateScoreMin());
        }
        if (query.getDuplicateScoreMax() != null) {
            wrapper.le(Project::getDuplicateScore, query.getDuplicateScoreMax());
        }
        if (query.getCreatedFrom() != null) {
            wrapper.ge(Project::getCreatedAt, query.getCreatedFrom().atStartOfDay());
        }
        if (query.getCreatedTo() != null) {
            wrapper.le(Project::getCreatedAt, query.getCreatedTo().atTime(23, 59, 59));
        }

        applySort(wrapper, query);
        long pageNo = Math.max(1L, query.getPage());
        long pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1L, query.getSize()));
        Page<Project> page = projectMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<ProjectListItemVO> records = toListItems(page.getRecords());
        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public ProjectDetailVO detail(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        accessService.assertCanView(project);

        ProjectDetailVO vo = new ProjectDetailVO();
        copyBase(project, vo);
        vo.setGroupName(resolveGroupName(project.getGroupId()));
        vo.setOwnerName(resolveUserName(project.getOwnerId()));
        vo.setTechDirectionName(resolveDirectionName(project.getTechDirectionId()));
        vo.setGoals(goalMapper.selectList(Wrappers.<com.taiji.fund.project.domain.ProjectGoal>lambdaQuery()
                        .eq(com.taiji.fund.project.domain.ProjectGoal::getProjectId, id)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectGoal::getSort)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectGoal::getId))
                .stream()
                .map(ProjectGoalVO::from)
                .collect(Collectors.toList()));
        vo.setContents(contentMapper.selectList(Wrappers.<com.taiji.fund.project.domain.ProjectContent>lambdaQuery()
                        .eq(com.taiji.fund.project.domain.ProjectContent::getProjectId, id)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectContent::getSort)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectContent::getId))
                .stream()
                .map(ProjectContentVO::from)
                .collect(Collectors.toList()));
        vo.setKeyTechs(keyTechMapper.selectList(Wrappers.<com.taiji.fund.project.domain.ProjectKeyTech>lambdaQuery()
                        .eq(com.taiji.fund.project.domain.ProjectKeyTech::getProjectId, id)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectKeyTech::getSort)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectKeyTech::getId))
                .stream()
                .map(ProjectKeyTechVO::from)
                .collect(Collectors.toList()));
        vo.setMilestones(milestoneMapper.selectList(Wrappers.<com.taiji.fund.project.domain.ProjectMilestone>lambdaQuery()
                        .eq(com.taiji.fund.project.domain.ProjectMilestone::getProjectId, id)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectMilestone::getSort)
                        .orderByAsc(com.taiji.fund.project.domain.ProjectMilestone::getId))
                .stream()
                .map(ProjectMilestoneVO::from)
                .collect(Collectors.toList()));
        vo.setMembers(memberMapper.selectList(Wrappers.<com.taiji.fund.project.domain.ProjectMember>lambdaQuery()
                        .eq(com.taiji.fund.project.domain.ProjectMember::getProjectId, id))
                .stream()
                .map(ProjectMemberVO::from)
                .collect(Collectors.toList()));
        vo.setApprovalLogs(approvalLogMapper.selectList(Wrappers
                .<com.taiji.fund.project.domain.ProjectApprovalLog>lambdaQuery()
                .eq(com.taiji.fund.project.domain.ProjectApprovalLog::getProjectId, id)
                .orderByAsc(com.taiji.fund.project.domain.ProjectApprovalLog::getId))
                .stream()
                .map(ProjectApprovalLogVO::from)
                .collect(Collectors.toList()));
        vo.setReviewRound(project.getReviewRound());
        vo.setReviewPassCount(reviewService.passCount(id));
        vo.setRequiredPassCount(reviewConfigService.requiredPassCount());
        List<com.taiji.fund.project.domain.ProjectReview> reviews = reviewMapper.selectList(Wrappers
                .<com.taiji.fund.project.domain.ProjectReview>lambdaQuery()
                .eq(com.taiji.fund.project.domain.ProjectReview::getProjectId, id)
                .orderByDesc(com.taiji.fund.project.domain.ProjectReview::getRound)
                .orderByAsc(com.taiji.fund.project.domain.ProjectReview::getId));
        Map<Long, String> reviewerNames = reviews.isEmpty() ? Collections.emptyMap()
                : userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .in(User::getId, reviews.stream()
                                .map(com.taiji.fund.project.domain.ProjectReview::getReviewerId)
                                .filter(reviewerId -> reviewerId != null)
                                .distinct().collect(Collectors.toList())))
                .stream().collect(Collectors.toMap(User::getId,
                        user -> user.getRealName() == null ? user.getUsername() : user.getRealName(),
                        (left, right) -> left));
        vo.setReviews(reviews.stream().map(review -> {
            ProjectReviewVO reviewVO = ProjectReviewVO.from(review);
            reviewVO.setReviewerName(reviewerNames.get(review.getReviewerId()));
            return reviewVO;
        }).collect(Collectors.toList()));
        vo.setDocuments(documentMapper.selectList(Wrappers
                .<com.taiji.fund.project.domain.ProjectDocument>lambdaQuery()
                .eq(com.taiji.fund.project.domain.ProjectDocument::getProjectId, id)
                .orderByDesc(com.taiji.fund.project.domain.ProjectDocument::getId))
                .stream()
                .map(this::toDocumentVO)
                .collect(Collectors.toList()));
        return vo;
    }

    public Project loadForAccess(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        accessService.assertCanView(project);
        return project;
    }

    private void applyScope(LambdaQueryWrapper<Project> wrapper) {
        LoginUser user = SecurityUtils.currentUserOrNull();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getRoles().contains(RoleCode.ADMIN.name())
                || user.getRoles().contains(RoleCode.PMO.name())) {
            return;
        }
        if (user.getRoles().contains(RoleCode.GROUP_USER.name())) {
            applyGroupScope(wrapper, user);
            return;
        }
        if (user.getRoles().contains(RoleCode.EXPERT.name())) {
            wrapper.and(condition -> {
                condition.eq(Project::getStatus, ProjectStatus.REVIEWING.getCode());
                if (user.getGroupId() != null) {
                    condition.or(inner -> inner.eq(Project::getGroupId, user.getGroupId())
                            .ne(Project::getStatus, ProjectStatus.DRAFT.getCode()));
                }
            });
            return;
        }
        wrapper.eq(Project::getId, -1L);
    }

    private void applyGroupScope(LambdaQueryWrapper<Project> wrapper, LoginUser user) {
        if (user.getGroupId() == null) {
            wrapper.eq(Project::getId, -1L);
        } else {
            wrapper.eq(Project::getGroupId, user.getGroupId());
        }
    }

    private void applySort(LambdaQueryWrapper<Project> wrapper, ProjectQuery query) {
        boolean asc = "asc".equalsIgnoreCase(query.getSortOrder());
        String sortBy = query.getSortBy() == null ? "updatedAt" : query.getSortBy();
        switch (sortBy) {
            case "budget":
                wrapper.orderBy(true, asc, Project::getBudget);
                break;
            case "name":
                wrapper.orderBy(true, asc, Project::getName);
                break;
            case "createdAt":
                wrapper.orderBy(true, asc, Project::getCreatedAt);
                break;
            case "submitTime":
                wrapper.orderBy(true, asc, Project::getSubmitTime);
                break;
            case "duplicateScore":
                wrapper.orderBy(true, asc, Project::getDuplicateScore);
                break;
            case "updatedAt":
            default:
                wrapper.orderBy(true, asc, Project::getUpdatedAt);
                break;
        }
        wrapper.orderByDesc(Project::getId);
    }

    private List<ProjectListItemVO> toListItems(List<Project> projects) {
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> groupIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> directionIds = new LinkedHashSet<>();
        for (Project project : projects) {
            if (project.getGroupId() != null) {
                groupIds.add(project.getGroupId());
            }
            if (project.getOwnerId() != null) {
                userIds.add(project.getOwnerId());
            }
            if (project.getTechDirectionId() != null) {
                directionIds.add(project.getTechDirectionId());
            }
        }
        Map<Long, String> groupNames = groupIds.isEmpty() ? Collections.emptyMap()
                : groupMapper.selectBatchIds(groupIds).stream()
                .collect(Collectors.toMap(Group::getId, Group::getName));
        Map<Long, String> userNames = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user.getRealName() == null
                        ? user.getUsername() : user.getRealName()));
        Map<Long, String> directionNames = directionIds.isEmpty() ? Collections.emptyMap()
                : techDirectionMapper.selectBatchIds(directionIds).stream()
                .collect(Collectors.toMap(TechDirection::getId, TechDirection::getName));

        List<ProjectListItemVO> result = new ArrayList<>(projects.size());
        for (Project project : projects) {
            ProjectListItemVO vo = new ProjectListItemVO();
            vo.setId(project.getId());
            vo.setCode(project.getCode());
            vo.setName(project.getName());
            vo.setFundType(project.getFundType());
            vo.setGroupId(project.getGroupId());
            vo.setGroupName(groupNames.get(project.getGroupId()));
            vo.setOwnerId(project.getOwnerId());
            vo.setOwnerName(userNames.get(project.getOwnerId()));
            vo.setTechDirectionId(project.getTechDirectionId());
            vo.setTechDirectionName(directionNames.get(project.getTechDirectionId()));
            vo.setBudget(project.getBudget());
            vo.setStartDate(project.getStartDate());
            vo.setEndDate(project.getEndDate());
            vo.setStatus(project.getStatus());
            vo.setDuplicateScore(project.getDuplicateScore());
            vo.setRiskLevel(project.getRiskLevel());
            vo.setSubmitTime(project.getSubmitTime());
            vo.setUpdatedAt(project.getUpdatedAt());
            vo.setVersion(project.getVersion());
            result.add(vo);
        }
        return result;
    }

    private void copyBase(Project project, ProjectDetailVO vo) {
        vo.setId(project.getId());
        vo.setCode(project.getCode());
        vo.setName(project.getName());
        vo.setFundType(project.getFundType());
        vo.setGroupId(project.getGroupId());
        vo.setOwnerId(project.getOwnerId());
        vo.setTechDirectionId(project.getTechDirectionId());
        vo.setBudget(project.getBudget());
        vo.setStartDate(project.getStartDate());
        vo.setEndDate(project.getEndDate());
        vo.setStatus(project.getStatus());
        vo.setSummary(project.getSummary());
        vo.setExpectedOutcome(project.getExpectedOutcome());
        vo.setDuplicateScore(project.getDuplicateScore());
        vo.setRiskLevel(project.getRiskLevel());
        vo.setSubmitTime(project.getSubmitTime());
        vo.setCreatedAt(project.getCreatedAt());
        vo.setUpdatedAt(project.getUpdatedAt());
        vo.setVersion(project.getVersion());
    }

    private ProjectDocumentVO toDocumentVO(com.taiji.fund.project.domain.ProjectDocument document) {
        ProjectDocumentVO vo = new ProjectDocumentVO();
        vo.setId(document.getId());
        vo.setProjectId(document.getProjectId());
        vo.setFileName(document.getFileName());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setParseStatus(document.getParseStatus());
        vo.setParseMessage(document.getParseMessage());
        vo.setUploadedBy(document.getUploadedBy());
        vo.setCreatedAt(document.getCreatedAt());
        return vo;
    }

    private String resolveGroupName(Long id) {
        if (id == null) {
            return null;
        }
        Group group = groupMapper.selectById(id);
        return group == null ? null : group.getName();
    }

    private String resolveUserName(Long id) {
        if (id == null) {
            return null;
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return null;
        }
        return user.getRealName() == null ? user.getUsername() : user.getRealName();
    }

    private String resolveDirectionName(Long id) {
        if (id == null) {
            return null;
        }
        TechDirection direction = techDirectionMapper.selectById(id);
        return direction == null ? null : direction.getName();
    }

    private void validateFundType(String fundType) {
        if (!FundType.PLATFORM.getCode().equals(fundType)
                && !FundType.INNOVATION.getCode().equals(fundType)
                && !FundType.OTHER.getCode().equals(fundType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "基金类型不合法");
        }
    }

    private void validateStatuses(List<String> statuses) {
        for (String status : statuses) {
            boolean valid = false;
            for (ProjectStatus value : ProjectStatus.values()) {
                if (value.getCode().equals(status)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                throw new BizException(ErrorCode.BAD_REQUEST, "项目状态不合法");
            }
        }
    }
}

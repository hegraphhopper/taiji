package com.taiji.fund.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectReview;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectReviewMapper;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 待办：按角色与项目状态汇总，GROUP_USER 只看本组，专家只看未评审的 REVIEWING 项目。
 */
@Service
@RequiredArgsConstructor
public class TodoService {

    private static final int LIMIT = 200;

    private final ProjectMapper projectMapper;
    private final ProjectReviewMapper reviewMapper;
    private final GroupMapper groupMapper;

    @Transactional(readOnly = true)
    public List<TodoItemVO> list() {
        LoginUser user = SecurityUtils.currentUser();
        Set<String> roles = new HashSet<>(user.getRoles());
        Map<Long, String> groupNames = groupNames();
        List<TodoItemVO> items = new ArrayList<>();

        if (roles.contains(RoleCode.ADMIN.name()) || roles.contains(RoleCode.PMO.name())) {
            items.addAll(businessItems(
                    Arrays.asList(ProjectStatus.SUBMITTED.getCode(),
                            ProjectStatus.REVIEWING.getCode()),
                    null, groupNames));
        }
        if (roles.contains(RoleCode.GROUP_USER.name())) {
            items.addAll(businessItems(
                    Arrays.asList(ProjectStatus.DRAFT.getCode(),
                            ProjectStatus.REJECTED.getCode()),
                    user.getGroupId(), groupNames));
        }
        if (roles.contains(RoleCode.EXPERT.name()) || roles.contains(RoleCode.ADMIN.name())) {
            items.addAll(reviewItems(user, groupNames));
        }
        return items.stream()
                .limit(LIMIT)
                .collect(Collectors.toList());
    }

    private List<TodoItemVO> businessItems(List<String> statuses, Long groupId,
                                           Map<Long, String> groupNames) {
        List<Project> projects = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                .in(Project::getStatus, statuses)
                .eq(groupId != null, Project::getGroupId, groupId)
                .orderByAsc(Project::getId));
        return projects.stream().map(project -> {
            String action;
            String label;
            String status = project.getStatus();
            if (ProjectStatus.SUBMITTED.getCode().equals(status)) {
                action = "ACCEPT";
                label = "形式审查";
            } else if (ProjectStatus.REVIEWING.getCode().equals(status)) {
                action = "APPROVE";
                label = "立项/驳回";
            } else if (ProjectStatus.REJECTED.getCode().equals(status)) {
                action = "RESUBMIT";
                label = "修改后重新提交";
            } else {
                action = "EDIT";
                label = "继续完善";
            }
            return TodoItemVO.builder()
                    .projectId(project.getId())
                    .code(project.getCode())
                    .name(project.getName())
                    .groupId(project.getGroupId())
                    .groupName(groupNames.get(project.getGroupId()))
                    .status(status)
                    .action(action)
                    .actionLabel(label)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<TodoItemVO> reviewItems(LoginUser user, Map<Long, String> groupNames) {
        List<Project> reviewing = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                .eq(Project::getStatus, ProjectStatus.REVIEWING.getCode())
                .orderByAsc(Project::getId));
        if (reviewing.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> projectIds = reviewing.stream().map(Project::getId).collect(Collectors.toList());
        List<ProjectReview> mine = reviewMapper.selectList(Wrappers.<ProjectReview>lambdaQuery()
                .in(ProjectReview::getProjectId, projectIds)
                .eq(ProjectReview::getReviewerId, user.getId()));
        Map<Long, Set<Integer>> reviewedRounds = new HashMap<>();
        for (ProjectReview review : mine) {
            reviewedRounds.computeIfAbsent(review.getProjectId(), key -> new HashSet<>())
                    .add(review.getRound());
        }
        List<TodoItemVO> items = new ArrayList<>();
        for (Project project : reviewing) {
            int round = project.getReviewRound() == null || project.getReviewRound() < 1
                    ? 1 : project.getReviewRound();
            Set<Integer> rounds = reviewedRounds.getOrDefault(project.getId(),
                    Collections.emptySet());
            if (rounds.contains(round)) {
                continue;
            }
            items.add(TodoItemVO.builder()
                    .projectId(project.getId())
                    .code(project.getCode())
                    .name(project.getName())
                    .groupId(project.getGroupId())
                    .groupName(groupNames.get(project.getGroupId()))
                    .status(project.getStatus())
                    .action("REVIEW")
                    .actionLabel("专家评审")
                    .build());
        }
        return items;
    }

    private Map<Long, String> groupNames() {
        return groupMapper.selectList(Wrappers.<Group>lambdaQuery())
                .stream().collect(Collectors.toMap(Group::getId, Group::getName, (l, r) -> l));
    }
}

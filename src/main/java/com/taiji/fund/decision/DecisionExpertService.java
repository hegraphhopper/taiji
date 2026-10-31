package com.taiji.fund.decision;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.enums.UserStatus;
import com.taiji.fund.decision.dto.SuggestedExpertVO;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectReview;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectReviewMapper;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.role.Role;
import com.taiji.fund.system.role.RoleMapper;
import com.taiji.fund.system.role.UserRole;
import com.taiji.fund.system.role.UserRoleMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 建议评审专家：同技术方向优先、进行中评审数升序，最多 3 人；无专家时回退基金办/管理员。
 */
@Service
@RequiredArgsConstructor
public class DecisionExpertService {

    private static final int MAX_EXPERTS = 3;

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;
    private final ProjectReviewMapper reviewMapper;
    private final ProjectMapper projectMapper;
    private final GroupMapper groupMapper;

    public List<SuggestedExpertVO> suggest(Long techDirectionId) {
        List<SuggestedExpertVO> experts = candidates(RoleCode.EXPERT, techDirectionId);
        if (!experts.isEmpty()) {
            return experts.size() > MAX_EXPERTS
                    ? new ArrayList<>(experts.subList(0, MAX_EXPERTS)) : experts;
        }
        List<SuggestedExpertVO> managers = candidates(RoleCode.PMO, techDirectionId);
        if (managers.isEmpty()) {
            managers = candidates(RoleCode.ADMIN, techDirectionId);
        }
        return managers.size() > MAX_EXPERTS
                ? new ArrayList<>(managers.subList(0, MAX_EXPERTS)) : managers;
    }

    private List<SuggestedExpertVO> candidates(RoleCode roleCode, Long techDirectionId) {
        Long roleId = roleId(roleCode);
        if (roleId == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = userRoleMapper.selectList(Wrappers.<UserRole>lambdaQuery()
                        .eq(UserRole::getRoleId, roleId))
                .stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .in(User::getId, userIds)
                .eq(User::getStatus, UserStatus.ENABLED.name())
                .orderByAsc(User::getId));
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> groupNames = groupNames(users);
        Map<Long, int[]> reviewStats = reviewStats(userIds, techDirectionId);
        List<SuggestedExpertVO> result = new ArrayList<>(users.size());
        for (User user : users) {
            int[] stats = reviewStats.getOrDefault(user.getId(), new int[]{0, 0});
            result.add(SuggestedExpertVO.builder()
                    .id(user.getId())
                    .realName(user.getRealName() == null ? user.getUsername() : user.getRealName())
                    .groupName(user.getGroupId() == null ? null : groupNames.get(user.getGroupId()))
                    .ongoingReviews(stats[0])
                    .sameDirectionReviews(stats[1])
                    .build());
        }
        result.sort(Comparator.comparing(SuggestedExpertVO::getSameDirectionReviews).reversed()
                .thenComparing(SuggestedExpertVO::getOngoingReviews)
                .thenComparing(SuggestedExpertVO::getRealName,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SuggestedExpertVO::getId));
        return result;
    }

    private Long roleId(RoleCode roleCode) {
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, roleCode.name()));
        return role == null ? null : role.getId();
    }

    private Map<Long, String> groupNames(List<User> users) {
        List<Long> groupIds = users.stream().map(User::getGroupId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return groupMapper.selectList(Wrappers.<Group>lambdaQuery().in(Group::getId, groupIds))
                .stream().collect(Collectors.toMap(Group::getId, Group::getName, (left, right) -> left));
    }

    /**
     * 返回 reviewerId -> [进行中评审数, 同方向历史评审数]。
     */
    private Map<Long, int[]> reviewStats(List<Long> reviewerIds, Long techDirectionId) {
        Map<Long, int[]> stats = new HashMap<>();
        if (reviewerIds.isEmpty()) {
            return stats;
        }
        List<ProjectReview> reviews = reviewMapper.selectList(Wrappers.<ProjectReview>lambdaQuery()
                .in(ProjectReview::getReviewerId, reviewerIds));
        if (reviews.isEmpty()) {
            return stats;
        }
        List<Long> projectIds = reviews.stream().map(ProjectReview::getProjectId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, Project> projects = projectIds.isEmpty() ? Collections.emptyMap()
                : projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                        .in(Project::getId, projectIds))
                .stream().collect(Collectors.toMap(Project::getId, project -> project, (l, r) -> l));
        for (ProjectReview review : reviews) {
            Project project = projects.get(review.getProjectId());
            if (project == null) {
                continue;
            }
            int[] value = stats.computeIfAbsent(review.getReviewerId(), key -> new int[]{0, 0});
            if (ProjectStatus.REVIEWING.getCode().equals(project.getStatus())) {
                value[0]++;
            }
            if (techDirectionId != null && techDirectionId.equals(project.getTechDirectionId())) {
                value[1]++;
            }
        }
        return stats;
    }
}

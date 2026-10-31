package com.taiji.fund.analysis.result;

import com.taiji.fund.analysis.result.dto.AnalysisProjectBriefVO;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 批量组装项目摘要（名称、项目组、负责人、技术方向），避免逐条查询造成 N+1。
 */
@Component
@RequiredArgsConstructor
public class ProjectBriefAssembler {

    private final ProjectMapper projectMapper;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final TechDirectionMapper techDirectionMapper;

    public AnalysisProjectBriefVO assemble(Project project) {
        if (project == null) {
            return null;
        }
        return assemble(Collections.singletonList(project)).get(project.getId());
    }

    public Map<Long, AnalysisProjectBriefVO> assemble(Collection<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> groupIds = projects.stream().map(Project::getGroupId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> userIds = projects.stream().map(Project::getOwnerId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> directionIds = projects.stream().map(Project::getTechDirectionId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, String> groupNames = new LinkedHashMap<>();
        if (!groupIds.isEmpty()) {
            for (Group group : groupMapper.selectBatchIds(groupIds)) {
                groupNames.put(group.getId(), group.getName());
            }
        }
        Map<Long, String> userNames = new LinkedHashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userNames.put(user.getId(),
                        user.getRealName() == null ? user.getUsername() : user.getRealName());
            }
        }
        Map<Long, String> directionNames = new LinkedHashMap<>();
        if (!directionIds.isEmpty()) {
            for (TechDirection direction : techDirectionMapper.selectBatchIds(directionIds)) {
                directionNames.put(direction.getId(), direction.getName());
            }
        }

        Map<Long, AnalysisProjectBriefVO> result = new LinkedHashMap<>();
        for (Project project : projects) {
            result.put(project.getId(), AnalysisProjectBriefVO.builder()
                    .id(project.getId())
                    .code(project.getCode())
                    .name(project.getName())
                    .groupId(project.getGroupId())
                    .groupName(groupNames.get(project.getGroupId()))
                    .ownerId(project.getOwnerId())
                    .ownerName(userNames.get(project.getOwnerId()))
                    .techDirectionId(project.getTechDirectionId())
                    .techDirectionName(directionNames.get(project.getTechDirectionId()))
                    .status(project.getStatus())
                    .budget(project.getBudget())
                    .submitTime(project.getSubmitTime())
                    .build());
        }
        return result;
    }

    public Map<Long, AnalysisProjectBriefVO> assembleByIds(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Project> projects = projectMapper.selectBatchIds(new LinkedHashSet<>(projectIds));
        return assemble(projects);
    }
}

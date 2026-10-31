package com.taiji.fund.analysis.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.tokenizer.TextNormalizer;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectContent;
import com.taiji.fund.project.domain.ProjectDocument;
import com.taiji.fund.project.domain.ProjectGoal;
import com.taiji.fund.project.domain.ProjectKeyTech;
import com.taiji.fund.project.domain.ProjectMilestone;
import com.taiji.fund.project.mapper.ProjectContentMapper;
import com.taiji.fund.project.mapper.ProjectDocumentMapper;
import com.taiji.fund.project.mapper.ProjectGoalMapper;
import com.taiji.fund.project.mapper.ProjectKeyTechMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectMilestoneMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从项目主数据与子表构建分析输入；结构化文本为空时回退附件解析文本。
 */
@Service
@RequiredArgsConstructor
public class ProjectAnalysisInputFactory {

    private final ProjectMapper projectMapper;
    private final ProjectGoalMapper goalMapper;
    private final ProjectContentMapper contentMapper;
    private final ProjectKeyTechMapper keyTechMapper;
    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectDocumentMapper documentMapper;
    private final TechDirectionMapper techDirectionMapper;

    @Transactional(readOnly = true)
    public ProjectAnalysisInput build(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        List<ProjectAnalysisInput> inputs = buildAll(Collections.singletonList(project));
        return inputs.get(0);
    }

    @Transactional(readOnly = true)
    public List<ProjectAnalysisInput> buildAll(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<ProjectGoal>> goals = groupGoals(projectIds);
        Map<Long, List<ProjectContent>> contents = groupContents(projectIds);
        Map<Long, List<ProjectKeyTech>> keyTechs = groupKeyTechs(projectIds);
        Map<Long, List<ProjectMilestone>> milestones = groupMilestones(projectIds);
        Map<Long, String> documentTexts = loadDocumentTexts(projectIds);
        Map<Long, String> directionNames = loadDirectionNames(projects);

        List<ProjectAnalysisInput> inputs = new ArrayList<>(projects.size());
        for (Project project : projects) {
            inputs.add(toInput(project, goals, contents, keyTechs, milestones, documentTexts, directionNames));
        }
        return inputs;
    }

    private ProjectAnalysisInput toInput(Project project,
                                         Map<Long, List<ProjectGoal>> goals,
                                         Map<Long, List<ProjectContent>> contents,
                                         Map<Long, List<ProjectKeyTech>> keyTechs,
                                         Map<Long, List<ProjectMilestone>> milestones,
                                         Map<Long, String> documentTexts,
                                         Map<Long, String> directionNames) {
        Long projectId = project.getId();
        String documentText = documentTexts.getOrDefault(projectId, "");

        String goalText = TextNormalizer.normalize(joinGoals(goals.get(projectId), documentText));
        String contentText = TextNormalizer.normalize(joinContents(contents.get(projectId), documentText));
        String keyTechText = TextNormalizer.normalize(joinKeyTechs(keyTechs.get(projectId), documentText));

        List<MilestoneInfo> milestoneInfos = new ArrayList<>();
        List<ProjectMilestone> projectMilestones =
                milestones.getOrDefault(projectId, Collections.emptyList());
        for (ProjectMilestone milestone : projectMilestones) {
            milestoneInfos.add(new MilestoneInfo(milestone.getName(), milestone.getPlanStart(),
                    milestone.getPlanEnd(), milestone.getDeliverable()));
        }
        String milestoneText = TextNormalizer.normalize(joinMilestones(projectMilestones));

        Set<String> tags = new LinkedHashSet<>();
        String directionName = directionNames.get(project.getTechDirectionId());
        addTag(tags, directionName);
        for (ProjectKeyTech keyTech : keyTechs.getOrDefault(projectId, Collections.emptyList())) {
            addTag(tags, keyTech.getName());
        }

        return ProjectAnalysisInput.builder()
                .projectId(projectId)
                .projectCode(project.getCode())
                .projectName(project.getName())
                .groupId(project.getGroupId())
                .techDirectionId(project.getTechDirectionId())
                .techDirectionName(directionName)
                .submitTime(project.getSubmitTime())
                .createdAt(project.getCreatedAt())
                .budget(project.getBudget())
                .goalText(goalText)
                .contentText(contentText)
                .keyTechText(keyTechText)
                .milestoneText(milestoneText)
                .documentText(TextNormalizer.normalize(documentText))
                .milestones(milestoneInfos)
                .tags(tags)
                .build();
    }

    private String joinGoals(List<ProjectGoal> goals, String fallback) {
        if (goals == null || goals.isEmpty()) {
            return fallback;
        }
        return join(goals.stream().map(ProjectGoal::getContent).collect(Collectors.toList()));
    }

    private String joinContents(List<ProjectContent> contents, String fallback) {
        if (contents == null || contents.isEmpty()) {
            return fallback;
        }
        return join(contents.stream().map(ProjectContent::getContent).collect(Collectors.toList()));
    }

    private String joinKeyTechs(List<ProjectKeyTech> keyTechs, String fallback) {
        if (keyTechs == null || keyTechs.isEmpty()) {
            return fallback;
        }
        List<String> parts = new ArrayList<>();
        for (ProjectKeyTech keyTech : keyTechs) {
            StringBuilder builder = new StringBuilder();
            if (keyTech.getName() != null) {
                builder.append(keyTech.getName());
            }
            if (keyTech.getDescription() != null && !keyTech.getDescription().trim().isEmpty()) {
                builder.append(' ').append(keyTech.getDescription());
            }
            parts.add(builder.toString());
        }
        return join(parts);
    }

    private String joinMilestones(List<ProjectMilestone> milestones) {
        if (milestones == null || milestones.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (ProjectMilestone milestone : milestones) {
            StringBuilder builder = new StringBuilder();
            if (milestone.getName() != null) {
                builder.append(milestone.getName());
            }
            if (milestone.getDeliverable() != null && !milestone.getDeliverable().trim().isEmpty()) {
                builder.append(' ').append(milestone.getDeliverable());
            }
            parts.add(builder.toString());
        }
        return join(parts);
    }

    private String join(List<String> parts) {
        if (parts == null) {
            return "";
        }
        return parts.stream()
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.joining(" "));
    }

    private void addTag(Set<String> tags, String value) {
        if (value == null) {
            return;
        }
        String tag = value.trim().toLowerCase(Locale.ROOT);
        if (!tag.isEmpty()) {
            tags.add(tag);
        }
    }

    private Map<Long, List<ProjectGoal>> groupGoals(List<Long> projectIds) {
        return goalMapper.selectList(Wrappers.<ProjectGoal>lambdaQuery()
                        .in(ProjectGoal::getProjectId, projectIds)
                        .orderByAsc(ProjectGoal::getSort)
                        .orderByAsc(ProjectGoal::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectGoal::getProjectId, LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, List<ProjectContent>> groupContents(List<Long> projectIds) {
        return contentMapper.selectList(Wrappers.<ProjectContent>lambdaQuery()
                        .in(ProjectContent::getProjectId, projectIds)
                        .orderByAsc(ProjectContent::getSort)
                        .orderByAsc(ProjectContent::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectContent::getProjectId, LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, List<ProjectKeyTech>> groupKeyTechs(List<Long> projectIds) {
        return keyTechMapper.selectList(Wrappers.<ProjectKeyTech>lambdaQuery()
                        .in(ProjectKeyTech::getProjectId, projectIds)
                        .orderByAsc(ProjectKeyTech::getSort)
                        .orderByAsc(ProjectKeyTech::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectKeyTech::getProjectId, LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, List<ProjectMilestone>> groupMilestones(List<Long> projectIds) {
        return milestoneMapper.selectList(Wrappers.<ProjectMilestone>lambdaQuery()
                        .in(ProjectMilestone::getProjectId, projectIds)
                        .orderByAsc(ProjectMilestone::getSort)
                        .orderByAsc(ProjectMilestone::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectMilestone::getProjectId, LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, String> loadDocumentTexts(List<Long> projectIds) {
        List<ProjectDocument> documents = documentMapper.selectList(Wrappers.<ProjectDocument>lambdaQuery()
                .in(ProjectDocument::getProjectId, projectIds)
                .orderByAsc(ProjectDocument::getId));
        Map<Long, StringBuilder> builders = new LinkedHashMap<>();
        for (ProjectDocument document : documents) {
            String text = document.getTextContent();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }
            builders.computeIfAbsent(document.getProjectId(), key -> new StringBuilder())
                    .append(' ').append(text);
        }
        Map<Long, String> result = new LinkedHashMap<>();
        for (Map.Entry<Long, StringBuilder> entry : builders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString().trim());
        }
        return result;
    }

    private Map<Long, String> loadDirectionNames(List<Project> projects) {
        Set<Long> directionIds = projects.stream()
                .map(Project::getTechDirectionId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (directionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (TechDirection direction : techDirectionMapper.selectBatchIds(directionIds)) {
            names.put(direction.getId(), direction.getName());
        }
        return names;
    }
}

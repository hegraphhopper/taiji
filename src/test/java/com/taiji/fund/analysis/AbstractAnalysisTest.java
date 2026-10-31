package com.taiji.fund.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.AnalysisPairMapper;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.AnalysisRunMapper;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.DuplicateSnapshotMapper;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectContent;
import com.taiji.fund.project.domain.ProjectGoal;
import com.taiji.fund.project.domain.ProjectKeyTech;
import com.taiji.fund.project.domain.ProjectMilestone;
import com.taiji.fund.project.mapper.ProjectContentMapper;
import com.taiji.fund.project.mapper.ProjectGoalMapper;
import com.taiji.fund.project.mapper.ProjectKeyTechMapper;
import com.taiji.fund.project.mapper.ProjectMilestoneMapper;
import com.taiji.fund.project.AbstractProjectApiTest;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * A3 分析测试基类：直接通过 Mapper 造项目与申报内容，避免依赖前端与状态流转。
 */
public abstract class AbstractAnalysisTest extends AbstractProjectApiTest {

    protected static final String DEFAULT_GOAL = "面向工业场景的智能分析平台研究，构建数据采集与特征提取能力";
    protected static final String DEFAULT_CONTENT = "构建多模态数据采集、标注、训练与推理服务，支持实时监测与异常预警";
    protected static final String DEFAULT_TECH = "多模态特征融合 知识图谱推理 边缘侧低时延推理";
    protected static final String DEFAULT_MILESTONE = "原型开发";

    @Autowired
    protected ProjectGoalMapper goalMapper;

    @Autowired
    protected ProjectContentMapper contentMapper;

    @Autowired
    protected ProjectKeyTechMapper keyTechMapper;

    @Autowired
    protected ProjectMilestoneMapper milestoneMapper;

    @Autowired
    protected AnalysisRunMapper analysisRunMapper;

    @Autowired
    protected AnalysisPairMapper analysisPairMapper;

    @Autowired
    protected DuplicateSnapshotMapper snapshotMapper;

    protected Long ownerId() {
        return findByUsername("user01").getId();
    }

    protected long buildProject(Long groupId, String name, String goal, String content, String tech,
                                String milestoneName, LocalDate planStart, LocalDate planEnd,
                                BigDecimal budget, LocalDateTime submitTime, String status) {
        Project project = new Project();
        project.setCode(uniqueCode("AN"));
        project.setName(name);
        project.setFundType("PLATFORM");
        project.setGroupId(groupId);
        project.setOwnerId(ownerId());
        project.setTechDirectionId(firstTechDirectionId());
        project.setBudget(budget);
        project.setStartDate(planStart);
        project.setEndDate(planEnd);
        project.setStatus(status == null ? ProjectStatus.SUBMITTED.getCode() : status);
        project.setSubmitTime(submitTime);
        project.setCreatedBy(ownerId());
        project.setDeleted(0);
        project.setVersion(0);
        projectMapper.insert(project);
        insertGoal(project.getId(), goal);
        insertContent(project.getId(), content);
        insertKeyTech(project.getId(), tech);
        insertMilestone(project.getId(), milestoneName, planStart, planEnd);
        return project.getId();
    }

    protected long buildProject(Long groupId, String name, String goal, String content, String tech,
                                String milestoneName, BigDecimal budget, LocalDateTime submitTime,
                                String status) {
        return buildProject(groupId, name, goal, content, tech, milestoneName,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), budget, submitTime, status);
    }

    protected long buildProject(Long groupId, String name, LocalDateTime submitTime) {
        return buildProject(groupId, name, DEFAULT_GOAL, DEFAULT_CONTENT, DEFAULT_TECH,
                DEFAULT_MILESTONE, new BigDecimal("1000000.00"), submitTime,
                ProjectStatus.SUBMITTED.getCode());
    }

    protected void insertGoal(Long projectId, String content) {
        ProjectGoal goal = new ProjectGoal();
        goal.setProjectId(projectId);
        goal.setContent(content);
        goal.setSort(0);
        goalMapper.insert(goal);
    }

    protected void insertContent(Long projectId, String content) {
        ProjectContent row = new ProjectContent();
        row.setProjectId(projectId);
        row.setContent(content);
        row.setSort(0);
        contentMapper.insert(row);
    }

    protected void insertKeyTech(Long projectId, String tech) {
        String[] names = tech == null ? new String[0] : tech.split("\\s+");
        int sort = 0;
        for (String name : names) {
            if (name.trim().isEmpty()) {
                continue;
            }
            ProjectKeyTech keyTech = new ProjectKeyTech();
            keyTech.setProjectId(projectId);
            keyTech.setName(name.trim());
            keyTech.setDescription(name.trim());
            keyTech.setDifficulty("MEDIUM");
            keyTech.setSort(sort++);
            keyTechMapper.insert(keyTech);
        }
    }

    protected void insertMilestone(Long projectId, String name, LocalDate start, LocalDate end) {
        ProjectMilestone milestone = new ProjectMilestone();
        milestone.setProjectId(projectId);
        milestone.setName(name);
        milestone.setPlanStart(start);
        milestone.setPlanEnd(end);
        milestone.setDeliverable(name + "交付物");
        milestone.setWeight(100);
        milestone.setSort(0);
        milestoneMapper.insert(milestone);
    }

    protected MvcResult triggerRun(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/analysis/runs")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    protected JsonNode triggerRunData(String token, String body) throws Exception {
        MvcResult result = triggerRun(token, body);
        return readJson(result);
    }

    protected List<AnalysisPair> pairsOfRun(Long runId) {
        return analysisPairMapper.selectList(Wrappers.<AnalysisPair>lambdaQuery()
                .eq(AnalysisPair::getRunId, runId)
                .orderByAsc(AnalysisPair::getId));
    }

    protected List<DuplicateSnapshot> snapshotsOfRun(Long runId) {
        return snapshotMapper.selectList(Wrappers.<DuplicateSnapshot>lambdaQuery()
                .eq(DuplicateSnapshot::getRunId, runId)
                .orderByAsc(DuplicateSnapshot::getProjectId));
    }

    protected AnalysisRun latestRun() {
        List<AnalysisRun> runs = analysisRunMapper.selectList(
                Wrappers.<AnalysisRun>lambdaQuery().orderByDesc(AnalysisRun::getId));
        return runs.isEmpty() ? null : runs.get(0);
    }

    protected AnalysisRun insertRun(String triggerType, String status, Long projectId) {
        AnalysisRun run = new AnalysisRun();
        run.setTriggerType(triggerType);
        run.setStatus(status);
        run.setProjectId(projectId);
        run.setConfigVersion(1);
        run.setProjectCount(0);
        run.setPairCount(0);
        run.setLlmUsed(0);
        run.setOperatorId(1L);
        analysisRunMapper.insert(run);
        return run;
    }
}

package com.taiji.fund.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectContent;
import com.taiji.fund.project.domain.ProjectGoal;
import com.taiji.fund.project.domain.ProjectKeyTech;
import com.taiji.fund.project.domain.ProjectMilestone;
import com.taiji.fund.project.mapper.ProjectContentMapper;
import com.taiji.fund.project.mapper.ProjectGoalMapper;
import com.taiji.fund.project.mapper.ProjectKeyTechMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectMilestoneMapper;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 演示历史数据：为趋势看板准备近 8 个月的已提交项目。
 *
 * <p>仅在 demo profile 且 {@code app.demo.trend-seed=true} 时执行；按编码前缀幂等，重复启动不会重复插入。
 * 这些项目提交时间被回填到历史月份，配合 {@code scripts/backfill-trend.ps1} 逐月运行 analysisDate 分析，
 * 生成真实的月度快照，供 A4 趋势与归因演示。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoTrendSeedRunner implements ApplicationRunner {

    private static final String CODE_PREFIX = "DEMO-TREND-";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final Environment environment;
    private final ProjectMapper projectMapper;
    private final ProjectGoalMapper goalMapper;
    private final ProjectContentMapper contentMapper;
    private final ProjectKeyTechMapper keyTechMapper;
    private final ProjectMilestoneMapper milestoneMapper;
    private final GroupMapper groupMapper;
    private final TechDirectionMapper techDirectionMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!isDemoProfile() || isTestProfile()
                || !Boolean.parseBoolean(environment.getProperty("app.demo.trend-seed"))) {
            return;
        }
        Long existing = projectMapper.selectCount(Wrappers.<Project>lambdaQuery()
                .likeRight(Project::getCode, CODE_PREFIX));
        if (existing != null && existing > 0) {
            return;
        }
        User owner = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, "user01"));
        if (owner == null) {
            log.warn("缺少演示账号 user01，跳过趋势历史数据初始化");
            return;
        }
        List<Group> groups = groupMapper.selectList(Wrappers.<Group>lambdaQuery().orderByAsc(Group::getSort));
        List<TechDirection> directions = techDirectionMapper.selectList(
                Wrappers.<TechDirection>lambdaQuery().orderByAsc(TechDirection::getSort));
        if (groups.size() < 4 || directions.size() < 5) {
            log.warn("项目组或技术方向种子不足，跳过趋势历史数据初始化");
            return;
        }

        List<Seed> seeds = buildSeeds();
        YearMonth now = YearMonth.now(ZONE);
        int index = 0;
        for (Seed seed : seeds) {
            Group group = groups.get(seed.groupIndex % groups.size());
            TechDirection direction = directions.get(seed.directionIndex % directions.size());
            YearMonth submitMonth = now.minusMonths(seeds.size() - index);
            LocalDate submitDate = submitMonth.atDay(5);
            LocalDate start = submitDate.minusMonths(1);
            Project project = new Project();
            project.setCode(String.format("%s%02d", CODE_PREFIX, index + 1));
            project.setName(seed.name);
            project.setFundType(index % 3 == 0 ? "INNOVATION" : "PLATFORM");
            project.setGroupId(group.getId());
            project.setOwnerId(owner.getId());
            project.setTechDirectionId(direction.getId());
            project.setBudget(new BigDecimal(seed.budget));
            project.setStartDate(start);
            project.setEndDate(start.plusMonths(12));
            project.setStatus(ProjectStatus.SUBMITTED.getCode());
            project.setSummary(seed.goal);
            project.setExpectedOutcome(seed.content);
            project.setSubmitTime(submitDate.atTime(9, 30));
            project.setCreatedBy(owner.getId());
            project.setDeleted(0);
            project.setVersion(0);
            projectMapper.insert(project);
            insertSections(project.getId(), seed, start);
            index++;
        }
        log.info("已初始化 {} 个趋势演示历史项目（提交时间回填近 {} 个月）", seeds.size(), seeds.size());
    }

    private void insertSections(Long projectId, Seed seed, LocalDate start) {
        ProjectGoal goal = new ProjectGoal();
        goal.setProjectId(projectId);
        goal.setContent(seed.goal);
        goal.setSort(0);
        goalMapper.insert(goal);

        ProjectContent content = new ProjectContent();
        content.setProjectId(projectId);
        content.setContent(seed.content);
        content.setSort(0);
        contentMapper.insert(content);

        for (String tech : seed.techs) {
            ProjectKeyTech keyTech = new ProjectKeyTech();
            keyTech.setProjectId(projectId);
            keyTech.setName(tech);
            keyTech.setDescription(tech + "关键技术研究与验证");
            keyTech.setDifficulty("HIGH");
            keyTech.setSort(seed.techs.indexOf(tech));
            keyTechMapper.insert(keyTech);
        }

        String[] phases = {"需求与方案设计", "原型研发", "验证与推广"};
        for (int position = 0; position < phases.length; position++) {
            ProjectMilestone milestone = new ProjectMilestone();
            milestone.setProjectId(projectId);
            milestone.setName(phases[position]);
            milestone.setPlanStart(start.plusMonths(position * 4L));
            milestone.setPlanEnd(start.plusMonths(position * 4L + 3));
            milestone.setDeliverable(phases[position] + "交付物");
            milestone.setWeight(position == 0 ? 20 : (position == 1 ? 50 : 30));
            milestone.setSort(position);
            milestoneMapper.insert(milestone);
        }
    }

    private List<Seed> buildSeeds() {
        String kgGoal = "构建支持文本图像视频的多模态知识图谱，实现实体对齐、关系抽取与图谱质量评估，支撑智能问答与辅助决策。";
        String kgContent = "研究多模态实体识别与对齐方法，设计统一的知识表示模型，构建跨模态知识图谱存储与检索引擎，研发图谱质量评估与增量更新机制。";
        String dataGoal = "建设面向工业场景的数据治理平台，实现数据标准、数据质量、数据血缘与指标口径的统一管理。";
        String dataContent = "研发数据标准管理、质量规则引擎与数据血缘分析能力，构建工业数据资产目录与指标口径管理，支撑经营分析。";
        String zeroTrustGoal = "研究零信任架构下的细粒度访问控制与动态授权机制，构建身份可信评估与持续验证能力。";
        String zeroTrustContent = "研发身份可信评估、动态访问控制策略引擎与全链路审计能力，支持多租户隔离与终端持续验证。";
        String edgeGoal = "研发面向边缘场景的轻量推理框架与云边协同调度能力，降低模型部署与运行开销。";
        String edgeContent = "研究模型压缩与算子优化、边缘侧低时延推理调度、异构硬件适配与云边任务编排。";

        List<Seed> seeds = new ArrayList<>();
        seeds.add(new Seed("多模态知识图谱构建技术研究", 0, 0, "1200000.00",
                kgGoal, kgContent,
                Arrays.asList("多模态知识图谱", "深度学习", "实体对齐")));
        seeds.add(new Seed("多模态知识图谱推理技术研究", 1, 0, "900000.00",
                kgGoal, kgContent,
                Arrays.asList("多模态知识图谱", "深度学习", "知识推理")));
        seeds.add(new Seed("工业数据治理平台研发", 1, 1, "1500000.00",
                dataGoal, dataContent,
                Arrays.asList("数据治理", "数据质量", "数据血缘")));
        seeds.add(new Seed("工业数据治理与指标中台建设", 1, 1, "1100000.00",
                dataGoal, dataContent,
                Arrays.asList("数据治理", "数据质量", "指标中台")));
        seeds.add(new Seed("零信任访问控制技术研究", 2, 4, "800000.00",
                zeroTrustGoal, zeroTrustContent,
                Arrays.asList("零信任", "访问控制", "动态授权")));
        seeds.add(new Seed("零信任安全接入网关研制", 3, 4, "1000000.00",
                zeroTrustGoal, zeroTrustContent,
                Arrays.asList("零信任", "访问控制", "安全网关")));
        seeds.add(new Seed("边缘智能推理框架研发", 3, 3, "950000.00",
                edgeGoal, edgeContent,
                Arrays.asList("边缘计算", "低时延推理", "模型压缩")));
        seeds.add(new Seed("云边协同推理调度技术研究", 0, 3, "1050000.00",
                edgeGoal, edgeContent,
                Arrays.asList("边缘计算", "低时延推理", "云边协同")));
        return seeds;
    }

    private boolean isDemoProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("demo".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTestProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private static class Seed {
        private final String name;
        private final int groupIndex;
        private final int directionIndex;
        private final String budget;
        private final String goal;
        private final String content;
        private final List<String> techs;

        private Seed(String name, int groupIndex, int directionIndex, String budget,
                     String goal, String content, List<String> techs) {
            this.name = name;
            this.groupIndex = groupIndex;
            this.directionIndex = directionIndex;
            this.budget = budget;
            this.goal = goal;
            this.content = content;
            this.techs = techs;
        }
    }
}

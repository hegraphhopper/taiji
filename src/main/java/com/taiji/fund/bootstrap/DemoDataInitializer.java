package com.taiji.fund.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectContent;
import com.taiji.fund.project.domain.ProjectGoal;
import com.taiji.fund.project.domain.ProjectKeyTech;
import com.taiji.fund.project.domain.ProjectMember;
import com.taiji.fund.project.domain.ProjectMilestone;
import com.taiji.fund.project.mapper.ProjectContentMapper;
import com.taiji.fund.project.mapper.ProjectGoalMapper;
import com.taiji.fund.project.mapper.ProjectKeyTechMapper;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectMemberMapper;
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
 * 演示基线数据：60 个项目、3 个年度、8 个项目组，覆盖高/中/低重复与四类归因。
 *
 * <p>仅在 demo profile 且 {@code app.demo.data-seed=true} 时执行，按 {@code DEMO-} 前缀幂等。
 * 固定构造规则（无随机），提交时间分布在 2024-01 ~ 2026-09，供 14 个月趋势回填使用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private static final String CODE_PREFIX = "DEMO-";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int PAIR_COUNT = 10;
    private static final int UNIQUE_COUNT = 40;

    private static final String[] GOAL_TEMPLATES = {
            "面向{X}开展关键技术研究，重点解决{issue}问题，形成可复用的方法与工具。",
            "针对{X}的应用需求，研究其核心算法与工程实现，提升{scene}场景下的准确性与稳定性。",
            "围绕{X}构建技术体系，突破{issue}瓶颈，支撑{scene}业务的规模化应用。",
            "研究{X}的关键方法与评价体系，解决{issue}难题，形成{artifact}。",
            "面向{scene}场景，攻关{X}核心技术，提升系统性能与可靠性，输出{artifact}。",
            "开展{X}技术攻关与示范应用，重点解决{issue}，为{scene}提供可复制方案。"
    };

    private static final String[] CONTENT_TEMPLATES = {
            "完成{X}的需求分析、方案设计与原型开发，围绕{issue}开展算法优化与工程验证，形成{artifact}。",
            "研究{X}的核心算法与数据流程，建立测试基准，开展{scene}试点验证，沉淀{artifact}。",
            "构建{X}技术框架与工具链，突破{issue}，完成集成测试与现场验证，输出{artifact}。",
            "开展{X}关键技术研究，设计系统架构与接口规范，完成性能调优与安全加固，形成{artifact}。"
    };

    private static final String[] ISSUES = {
            "小样本条件下精度不足", "多源数据融合困难", "实时性无法满足业务要求",
            "复杂环境下鲁棒性不足", "跨系统协同效率低", "模型可解释性不足",
            "大规模数据存储与查询性能", "异构硬件适配困难", "安全与隐私保护",
            "工程化落地成本高"
    };

    private static final String[] SCENES = {
            "智能制造", "智慧园区", "能源电力", "轨道交通", "金融风控",
            "应急指挥", "遥感监测", "工业质检", "供应链协同", "科研试验"
    };

    private static final String[] ARTIFACTS = {
            "一套算法组件", "一套原型系统", "一份技术规范", "一套工具链",
            "一套评测基准", "一套示范应用", "一项专利与软件著作权", "一套部署方案"
    };

    private static final String[] UNIQUE_TOPICS = {
            "智能语音识别", "视频行为分析", "文本语义理解", "个性化推荐系统", "知识问答引擎",
            "图像缺陷检测", "三维场景重建", "时序数据预测", "强化学习决策", "联邦学习平台",
            "数据湖治理", "实时数仓建设", "数据质量评估", "主数据管理", "数据资产目录",
            "流批一体计算", "图数据分析", "数据脱敏", "数据血缘分析", "多源数据融合",
            "云原生调度", "容器安全", "微服务治理", "服务网格", "混沌工程",
            "可观测性平台", "多云管理", "边缘节点管理", "低代码平台", "流程自动化",
            "密码算法实现", "密钥管理服务", "安全审计", "入侵检测", "漏洞扫描",
            "软件供应链安全", "隐私计算", "可信执行环境", "区块链存证", "卫星导航增强"
    };

    private final Environment environment;
    private final ProjectMapper projectMapper;
    private final ProjectGoalMapper goalMapper;
    private final ProjectContentMapper contentMapper;
    private final ProjectKeyTechMapper keyTechMapper;
    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectMemberMapper memberMapper;
    private final GroupMapper groupMapper;
    private final TechDirectionMapper techDirectionMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!isDemoProfile() || isTestProfile()
                || !Boolean.parseBoolean(environment.getProperty("app.demo.data-seed"))) {
            return;
        }
        Long existing = projectMapper.selectCount(Wrappers.<Project>lambdaQuery()
                .likeRight(Project::getCode, CODE_PREFIX));
        if (existing != null && existing > 0) {
            log.info("演示数据已存在（{} 个项目），跳过初始化", existing);
            return;
        }
        User owner = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, "user01"));
        User participant = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, "expert01"));
        if (owner == null) {
            log.warn("缺少演示账号 user01，跳过演示数据初始化");
            return;
        }
        List<Group> groups = groupMapper.selectList(Wrappers.<Group>lambdaQuery().orderByAsc(Group::getSort));
        List<TechDirection> directions = techDirectionMapper.selectList(
                Wrappers.<TechDirection>lambdaQuery().orderByAsc(TechDirection::getSort));
        if (groups.size() < 8 || directions.size() < 10) {
            log.warn("项目组或技术方向种子不足（组 {} / 方向 {}），跳过演示数据初始化",
                    groups.size(), directions.size());
            return;
        }

        List<ProjectSeed> seeds = buildSeeds(groups, directions);
        int index = 0;
        for (ProjectSeed seed : seeds) {
            index++;
            Project project = new Project();
            project.setCode(String.format("%s%03d", CODE_PREFIX, index));
            project.setName(seed.name);
            project.setFundType(seed.fundType);
            project.setGroupId(seed.groupId);
            project.setOwnerId(owner.getId());
            project.setTechDirectionId(seed.directionId);
            project.setBudget(seed.budget);
            project.setStartDate(seed.startDate);
            project.setEndDate(seed.endDate);
            project.setStatus(seed.status);
            project.setSummary(seed.goal);
            project.setExpectedOutcome(String.join("；", seed.contents));
            project.setSubmitTime(seed.submitTime);
            project.setCreatedBy(owner.getId());
            project.setDeleted(0);
            project.setVersion(0);
            projectMapper.insert(project);
            insertSections(project.getId(), seed, owner, participant);
        }
        log.info("已初始化 {} 个演示项目（{} 个项目组，覆盖四类归因样本）", seeds.size(), groups.size());
    }

    private void insertSections(Long projectId, ProjectSeed seed, User owner, User participant) {
        ProjectGoal goal = new ProjectGoal();
        goal.setProjectId(projectId);
        goal.setContent(seed.goal);
        goal.setSort(0);
        goalMapper.insert(goal);

        for (int position = 0; position < seed.contents.size(); position++) {
            ProjectContent content = new ProjectContent();
            content.setProjectId(projectId);
            content.setContent(seed.contents.get(position));
            content.setSort(position);
            contentMapper.insert(content);
        }

        for (int position = 0; position < seed.techs.size(); position++) {
            ProjectKeyTech keyTech = new ProjectKeyTech();
            keyTech.setProjectId(projectId);
            keyTech.setName(seed.techs.get(position));
            keyTech.setDescription(seed.techs.get(position) + "的关键技术研究与工程验证");
            keyTech.setDifficulty(position == 0 ? "HIGH" : "MEDIUM");
            keyTech.setSort(position);
            keyTechMapper.insert(keyTech);
        }

        ProjectMember leader = new ProjectMember();
        leader.setProjectId(projectId);
        leader.setUserId(owner.getId());
        leader.setMemberRole("LEADER");
        leader.setWorkload(new BigDecimal("6.00"));
        memberMapper.insert(leader);

        if (participant != null) {
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(participant.getId());
            member.setMemberRole("PARTICIPANT");
            member.setWorkload(new BigDecimal("3.00"));
            memberMapper.insert(member);
        }

        String[] phases = {"需求与方案设计", "原型研发", "验证与推广"};
        for (int position = 0; position < phases.length; position++) {
            ProjectMilestone milestone = new ProjectMilestone();
            milestone.setProjectId(projectId);
            milestone.setName(phases[position]);
            milestone.setPlanStart(seed.startDate.plusMonths(position * 4L));
            milestone.setPlanEnd(seed.startDate.plusMonths(position * 4L + 3));
            milestone.setDeliverable(phases[position] + "阶段成果");
            milestone.setWeight(position == 0 ? 20 : (position == 1 ? 50 : 30));
            milestone.setSort(position);
            milestoneMapper.insert(milestone);
        }
    }

    private List<ProjectSeed> buildSeeds(List<Group> groups, List<TechDirection> directions) {
        List<ProjectSeed> seeds = new ArrayList<>();
        YearMonth now = YearMonth.now(ZONE);

        addPair(seeds, groups, directions, 0, 1,
                "多模态知识图谱构建技术研究", "多模态知识图谱推理技术研究",
                "构建支持文本图像视频的多模态知识图谱，实现实体对齐与关系抽取，支撑智能问答与辅助决策。",
                "研究多模态知识图谱推理机制，支持复杂查询与可解释推理，服务智能问答与辅助决策。",
                Arrays.asList("研究多模态实体识别与对齐方法，设计统一知识表示模型，构建跨模态知识图谱存储与检索引擎。",
                        "研发知识图谱质量评估与增量更新机制，支持大规模图谱持续演进。"),
                Arrays.asList("多模态知识图谱", "深度学习", "实体对齐"),
                new BigDecimal("1200000.00"), new BigDecimal("900000.00"),
                now.minusMonths(13), PairType.SELF);

        addPair(seeds, groups, directions, 2, 2,
                "工业数据治理平台研发", "工业数据治理与指标中台建设",
                "建设面向工业场景的数据治理平台，实现数据标准、数据质量、数据血缘与指标口径统一管理。",
                "建设工业数据指标中台，统一指标口径并支撑经营分析，沉淀可复用数据资产。",
                Arrays.asList("研发数据标准管理、质量规则引擎与数据血缘分析能力，构建工业数据资产目录。",
                        "建设指标口径管理与自助分析能力，支撑经营分析与决策。"),
                Arrays.asList("数据治理", "数据质量", "数据血缘"),
                new BigDecimal("1500000.00"), new BigDecimal("1100000.00"),
                now.minusMonths(12), PairType.SELF);

        addPair(seeds, groups, directions, 3, 4,
                "零信任访问控制技术研究", "零信任安全接入网关研制",
                "研究零信任架构下的细粒度访问控制与动态授权机制，构建身份可信评估能力。",
                "研制零信任安全接入网关，替代传统 VPN 实现细粒度访问控制与持续验证。",
                Arrays.asList("研发身份可信评估、动态访问控制策略引擎与全链路审计能力，支持多租户隔离。",
                        "研发加密隧道接入与终端持续验证能力，支持大规模并发接入。"),
                Arrays.asList("零信任", "访问控制", "动态授权"),
                new BigDecimal("800000.00"), new BigDecimal("1000000.00"),
                now.minusMonths(11), PairType.GAP);

        addPair(seeds, groups, directions, 4, 1,
                "边缘智能推理框架研发", "云边协同推理调度技术研究",
                "研发面向边缘场景的轻量推理框架，降低模型部署与运行开销。",
                "研究云边协同的推理任务调度方法，提升资源利用率与响应速度。",
                Arrays.asList("研究模型压缩与算子优化、边缘侧低时延推理调度，构建轻量推理运行时。",
                        "研究云边任务编排与弹性伸缩，支持异构硬件适配与运行时监控。"),
                Arrays.asList("边缘计算", "模型压缩", "低时延推理"),
                new BigDecimal("950000.00"), new BigDecimal("1050000.00"),
                now.minusMonths(10), PairType.GAP);

        addPair(seeds, groups, directions, 5, 6,
                "卫星遥感影像智能解译技术研究", "遥感影像变化检测技术研究",
                "研究卫星遥感影像智能解译方法，提升地物识别精度与处理效率。",
                "研究多时相遥感影像变化检测方法，支撑自然资源监测与应急评估。",
                Arrays.asList("构建遥感样本库与解译模型，研究小样本条件下的迁移学习与精度提升方法。",
                        "研究多时相影像配准与变化区域提取，形成变化检测技术方案。"),
                Arrays.asList("遥感影像", "智能解译", "变化检测"),
                new BigDecimal("1300000.00"), new BigDecimal("980000.00"),
                now.minusMonths(9), PairType.GAP);

        addPair(seeds, groups, directions, 6, 7,
                "电力设备故障预测技术研究", "电力设备健康管理平台研发",
                "研究电力设备故障预测方法，提升设备运行可靠性与运维效率。",
                "研发电力设备健康管理平台，实现状态评估、故障预警与检修决策支持。",
                Arrays.asList("研究设备多源监测数据融合与故障特征提取，构建故障预测模型。",
                        "构建设备健康度评估与检修决策模型，支持状态检修业务闭环。"),
                Arrays.asList("故障预测", "健康管理", "状态评估"),
                new BigDecimal("1400000.00"), new BigDecimal("1600000.00"),
                now.minusMonths(8), PairType.GAP);

        addPair(seeds, groups, directions, 3, 8,
                "网络安全态势感知平台研发", "网络安全威胁情报分析技术研究",
                "研发网络安全态势感知平台，实现资产发现、威胁检测与态势呈现。",
                "研究网络安全威胁情报分析技术，提升威胁发现与联动处置能力。",
                Arrays.asList("研究多源安全日志关联分析与威胁检测规则，构建态势感知平台。",
                        "研究威胁情报采集、关联与画像方法，支撑威胁研判与联动处置。"),
                Arrays.asList("态势感知", "威胁情报", "关联分析"),
                new BigDecimal("1250000.00"), new BigDecimal("870000.00"),
                now.minusMonths(7), PairType.GAP);

        addPair(seeds, groups, directions, 7, 5,
                "数字孪生工厂仿真技术研究", "产线数字孪生优化技术研究",
                "研究数字孪生工厂仿真技术，支撑产线设计与运行优化。",
                "研究产线数字孪生建模与优化方法，提升产线柔性与运行效率。",
                Arrays.asList("研究产线三维建模与仿真引擎，构建数字孪生工厂原型。",
                        "研究基于孪生模型的产线瓶颈分析与排产优化方法。"),
                Arrays.asList("数字孪生", "工厂仿真", "产线优化"),
                new BigDecimal("1150000.00"), new BigDecimal("1020000.00"),
                now.minusMonths(6), PairType.GAP);

        addPair(seeds, groups, directions, 6, 1,
                "密码应用合规检测技术研究", "密钥管理服务研制",
                "研究密码应用合规检测方法，支撑密码应用安全性评估。",
                "研制密钥管理服务，提供密钥全生命周期管理与密码服务能力。",
                Arrays.asList("研究密码算法实现与合规检测规则，构建检测工具与评估流程。",
                        "研究密钥生成、分发、轮换与销毁机制，构建高可用密钥管理服务。"),
                Arrays.asList("密码应用", "合规检测", "密钥管理"),
                new BigDecimal("760000.00"), new BigDecimal("1080000.00"),
                now.minusMonths(5), PairType.SIMULTANEOUS);

        addPair(seeds, groups, directions, 8, 2,
                "低时延通信协议栈优化技术研究", "工业通信协议适配技术研究",
                "研究低时延通信协议栈优化方法，提升工业网络实时性。",
                "研究工业通信协议适配技术，实现多协议互联与统一接入。",
                Arrays.asList("研究协议栈轻量化与调度优化，降低端到端时延与抖动。",
                        "研究多协议转换与适配框架，支持异构设备统一接入。"),
                Arrays.asList("通信协议", "低时延传输", "协议适配"),
                new BigDecimal("880000.00"), new BigDecimal("940000.00"),
                now.minusMonths(4), PairType.SIMULTANEOUS);

        YearMonth uniqueStart = YearMonth.of(2024, 1);
        for (int index = 0; index < UNIQUE_COUNT; index++) {
            String topic = UNIQUE_TOPICS[index];
            String issue = ISSUES[index % ISSUES.length];
            String scene = SCENES[(index / 2) % SCENES.length];
            String artifact = ARTIFACTS[index % ARTIFACTS.length];
            String goal = GOAL_TEMPLATES[index % GOAL_TEMPLATES.length]
                    .replace("{X}", topic).replace("{issue}", issue)
                    .replace("{scene}", scene).replace("{artifact}", artifact);
            String contentOne = CONTENT_TEMPLATES[index % CONTENT_TEMPLATES.length]
                    .replace("{X}", topic).replace("{issue}", issue)
                    .replace("{scene}", scene).replace("{artifact}", artifact);
            String contentTwo = "开展" + topic + "相关标准与测试方法研究，形成技术报告与实施建议。";

            YearMonth submitMonth = uniqueStart.plusMonths(Math.round(index * 31.0 / (UNIQUE_COUNT - 1)));
            String status = uniqueStatus(index);
            boolean draft = ProjectStatus.DRAFT.getCode().equals(status);
            Group group = groups.get(index % groups.size());
            TechDirection direction = directions.get(index % directions.size());
            BigDecimal budget = BigDecimal.valueOf(300000 + (index * 61000L) % 2700000L)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            LocalDate submitDate = submitMonth.atDay(5 + index % 20);
            seeds.add(new ProjectSeed(
                    topic + "关键技术研究与应用",
                    goal,
                    Arrays.asList(contentOne, contentTwo),
                    Arrays.asList(topic, ISSUES[(index + 3) % ISSUES.length], ARTIFACTS[(index + 1) % ARTIFACTS.length]),
                    group.getId(), direction.getId(),
                    index % 3 == 0 ? "INNOVATION" : (index % 7 == 0 ? "OTHER" : "PLATFORM"),
                    budget,
                    submitDate.minusMonths(1),
                    submitDate.minusMonths(1).plusMonths(12 + (index % 3) * 6),
                    status,
                    draft ? null : submitDate.atTime(9 + index % 8, 30)));
        }
        return seeds;
    }

    private void addPair(List<ProjectSeed> seeds, List<Group> groups, List<TechDirection> directions,
                         int groupIndexA, int groupIndexB, String nameA, String nameB,
                         String goalA, String goalB, List<String> contents, List<String> techs,
                         BigDecimal budgetA, BigDecimal budgetB, YearMonth base, PairType type) {
        Group groupA = groups.get(groupIndexA % groups.size());
        Group groupB = groups.get(groupIndexB % groups.size());
        TechDirection direction = directions.get((groupIndexA + groupIndexB) % directions.size());

        LocalDate submitA;
        LocalDate submitB;
        switch (type) {
            case SELF:
                submitA = base.minusMonths(1).atDay(5);
                submitB = base.atDay(5);
                break;
            case GAP:
                submitA = base.minusMonths(3).atDay(5);
                submitB = base.atDay(5);
                break;
            case SIMULTANEOUS:
            default:
                submitA = base.atDay(5);
                submitB = base.atDay(15);
                break;
        }
        String statusA = ProjectStatus.APPROVED.getCode();
        String statusB = ProjectStatus.IN_PROGRESS.getCode();
        seeds.add(pairSeed(nameA, goalA, contents, techs, groupA.getId(), direction.getId(),
                "PLATFORM", budgetA, submitA, statusA));
        seeds.add(pairSeed(nameB, goalB, contents, techs, groupB.getId(), direction.getId(),
                "INNOVATION", budgetB, submitB, statusB));
    }

    private ProjectSeed pairSeed(String name, String goal, List<String> contents, List<String> techs,
                                 Long groupId, Long directionId, String fundType, BigDecimal budget,
                                 LocalDate submitDate, String status) {
        return new ProjectSeed(name, goal, contents, techs, groupId, directionId, fundType, budget,
                submitDate.minusMonths(1), submitDate.minusMonths(1).plusMonths(18),
                status, submitDate.atTime(9, 30));
    }

    private String uniqueStatus(int index) {
        String[] statuses = {
                ProjectStatus.SUBMITTED.getCode(), ProjectStatus.REVIEWING.getCode(),
                ProjectStatus.APPROVED.getCode(), ProjectStatus.IN_PROGRESS.getCode(),
                ProjectStatus.COMPLETED.getCode(), ProjectStatus.REJECTED.getCode(),
                ProjectStatus.IN_PROGRESS.getCode(), ProjectStatus.APPROVED.getCode(),
                ProjectStatus.SUBMITTED.getCode(), ProjectStatus.DRAFT.getCode()
        };
        return statuses[index % statuses.length];
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

    private enum PairType {
        SELF, GAP, SIMULTANEOUS
    }

    private static class ProjectSeed {
        private final String name;
        private final String goal;
        private final List<String> contents;
        private final List<String> techs;
        private final Long groupId;
        private final Long directionId;
        private final String fundType;
        private final BigDecimal budget;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String status;
        private final LocalDateTime submitTime;

        private ProjectSeed(String name, String goal, List<String> contents, List<String> techs,
                            Long groupId, Long directionId, String fundType, BigDecimal budget,
                            LocalDate startDate, LocalDate endDate, String status,
                            LocalDateTime submitTime) {
            this.name = name;
            this.goal = goal;
            this.contents = contents;
            this.techs = techs;
            this.groupId = groupId;
            this.directionId = directionId;
            this.fundType = fundType;
            this.budget = budget;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.submitTime = submitTime;
        }
    }
}

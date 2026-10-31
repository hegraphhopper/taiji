package com.taiji.fund.insight.investment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.insight.trend.AbstractTrendTest;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B1 投资测试基类：直接造项目与快照，按年度/自建项目组隔离。
 */
public abstract class AbstractInvestmentTest extends AbstractTrendTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final List<Long> createdGroupIds = new ArrayList<>();

    private final List<Long> createdProjectIds = new ArrayList<>();

    @Autowired
    protected GroupMapper groupMapper;

    @AfterEach
    void cleanFixtures() {
        if (!createdProjectIds.isEmpty()) {
            snapshotMapper.delete(Wrappers.<DuplicateSnapshot>lambdaQuery()
                    .in(DuplicateSnapshot::getProjectId, createdProjectIds));
            createdProjectIds.forEach(projectMapper::deleteById);
        }
        createdGroupIds.forEach(groupMapper::deleteById);
        createdProjectIds.clear();
        createdGroupIds.clear();
    }

    protected Group createGroup(String name) {
        Group group = new Group();
        group.setCode("GRP-INV-" + System.nanoTime() + "-" + SEQUENCE.incrementAndGet());
        group.setName(name);
        group.setEnabled(1);
        group.setSort(99);
        group.setDeleted(0);
        groupMapper.insert(group);
        createdGroupIds.add(group.getId());
        return group;
    }

    protected Long insertProject(Long groupId, String name, String status, String fundType,
                                 LocalDate startDate, String budget) {
        Project project = new Project();
        project.setCode("INV-" + System.nanoTime() + "-" + SEQUENCE.incrementAndGet());
        project.setName(name);
        project.setFundType(fundType == null ? "PLATFORM" : fundType);
        project.setGroupId(groupId);
        project.setOwnerId(findByUsername("user01").getId());
        project.setTechDirectionId(firstTechDirectionId());
        project.setBudget(new BigDecimal(budget));
        project.setStartDate(startDate);
        project.setEndDate(startDate == null ? null : startDate.plusMonths(12));
        project.setStatus(status);
        project.setSubmitTime(LocalDateTime.now().minusMonths(3));
        project.setCreatedBy(project.getOwnerId());
        project.setDeleted(0);
        project.setVersion(0);
        projectMapper.insert(project);
        createdProjectIds.add(project.getId());
        return project.getId();
    }

    protected JsonNode overview(String token, String query) throws Exception {
        return getData("/api/v1/insight/investment/overview?" + query, token);
    }

    protected JsonNode investmentList(String token, String path, String query) throws Exception {
        return getData("/api/v1/insight/investment/" + path + "?" + query, token);
    }

    protected JsonNode getData(String url, String token) throws Exception {
        org.springframework.test.web.servlet.MvcResult result = mockMvc
                .perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }
}

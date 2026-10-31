package com.taiji.fund.insight.trend;

import com.taiji.fund.analysis.result.AnalysisRun;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT-20：60 项目 × 24 月（1440 快照）聚合性能。默认被 surefire 排除：
 * mvn test "-Dtest=TrendPerformanceTest" "-Dtest.excludedGroups=none"
 */
@Tag("perf")
class TrendPerformanceTest extends AbstractTrendTest {

    @Test
    void ut20AggregatesWithin500ms() throws Exception {
        Long group = intelGroupId();
        AnalysisRun run = newRun();
        int projects = 60;
        int months = 24;
        for (int index = 0; index < projects; index++) {
            Long projectId = newProject(group, "性能趋势项目" + index);
            for (int month = 0; month < months; month++) {
                LocalDate date = LocalDate.of(2020 + month / 12, month % 12 + 1, 10);
                snapshot(run.getId(), projectId, group, date,
                        String.valueOf(40 + (index + month) % 50) + ".00",
                        String.valueOf(30 + (index + month) % 40) + ".00",
                        (index + month) % 5 == 0 ? 1 : 0,
                        (index + month) % 3, 0, 0, 0);
            }
        }

        String token = pmoToken();
        long start = System.currentTimeMillis();
        trend(token, "startMonth=2020-01&endMonth=2021-12");
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[perf] A4 趋势聚合 60 项目 × 24 月耗时 " + elapsed + " ms");
        assertTrue(elapsed < 500, "聚合应 ≤500ms，实际 " + elapsed + "ms");
    }
}

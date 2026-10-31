package com.taiji.fund.insight.trend;

import com.taiji.fund.insight.trend.dto.AttributionCountsVO;
import com.taiji.fund.insight.trend.dto.TrendPointVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT-13：结论摘要数字与趋势点一致。
 */
class TrendSummaryTest {

    private final TrendSummaryService service = new TrendSummaryService();

    @Test
    void ut13SummaryMatchesPoints() {
        AttributionCountsVO counts = AttributionCountsVO.builder()
                .selfGroup(4).peerEarlier(3).ourEarlier(2).simultaneous(1).build();
        TrendPointVO first = TrendPointVO.builder()
                .period("2022-05")
                .avgMaxScore(new BigDecimal("50.00"))
                .attribution(AttributionCountsVO.builder().build())
                .ratios(AttributionCountsVO.builder().build().ratios())
                .build();
        TrendPointVO second = TrendPointVO.builder()
                .period("2022-06")
                .avgMaxScore(new BigDecimal("55.00"))
                .attribution(counts)
                .ratios(counts.ratios())
                .build();
        Map<String, Integer> groups = new LinkedHashMap<>();
        groups.put("智能计算项目组", 3);
        groups.put("数据智能项目组", 1);

        List<String> summary = service.summarize(Arrays.asList(first, second), groups);

        String joined = String.join("\n", summary);
        assertTrue(joined.contains("平均最高重复度 55.00"));
        assertTrue(joined.contains("上升"));
        assertTrue(joined.contains("10.0%"));
        assertTrue(joined.contains("本组内部重复"));
        assertTrue(joined.contains("40.0%"));
        assertTrue(joined.contains("智能计算项目组"));
    }

    @Test
    void ut13bEmptyAndSinglePoint() {
        assertTrue(service.summarize(Collections.<TrendPointVO>emptyList(), Collections.<String, Integer>emptyMap())
                .get(0).contains("暂无可分析数据"));

        TrendPointVO only = TrendPointVO.builder()
                .period("2022-06")
                .avgMaxScore(new BigDecimal("55.00"))
                .attribution(AttributionCountsVO.builder().build())
                .ratios(AttributionCountsVO.builder().build().ratios())
                .build();
        List<String> summary = service.summarize(Collections.singletonList(only),
                Collections.<String, Integer>emptyMap());
        assertTrue(String.join("\n", summary).contains("数据不足以判断趋势"));
    }
}

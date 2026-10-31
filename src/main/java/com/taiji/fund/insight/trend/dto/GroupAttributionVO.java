package com.taiji.fund.insight.trend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 按项目组拆分的归因矩阵行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupAttributionVO {

    private Long groupId;

    private String groupName;

    private AttributionCountsVO counts;

    private int total;
}

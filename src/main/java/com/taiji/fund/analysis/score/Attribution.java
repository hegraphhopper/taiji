package com.taiji.fund.analysis.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 归因结果：数据库存储中性事实（earlierProjectId），展示层再解释为我方/他方在先。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attribution {

    public static final String RELATION_SELF_GROUP = "SELF_GROUP";
    public static final String RELATION_CROSS_GROUP = "CROSS_GROUP";

    public static final String DIRECTION_SELF_GROUP = "SELF_GROUP";
    public static final String DIRECTION_SIMULTANEOUS = "SIMULTANEOUS";
    public static final String DIRECTION_A_EARLIER = "A_EARLIER";
    public static final String DIRECTION_B_EARLIER = "B_EARLIER";

    private String relation;

    private String direction;

    private Long earlierProjectId;
}

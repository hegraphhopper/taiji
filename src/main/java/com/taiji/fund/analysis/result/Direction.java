package com.taiji.fund.analysis.result;

/**
 * 存储中性方向事实：A_EARLIER 表示 project_id 一方在先，B_EARLIER 表示 peer_project_id 一方在先。
 * 展示层再按查看项目解释为 OUR_EARLIER / PEER_EARLIER。
 */
public enum Direction {

    SELF_GROUP,
    SIMULTANEOUS,
    A_EARLIER,
    B_EARLIER
}

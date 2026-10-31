package com.taiji.fund.analysis.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 节点计划明细：阶段名称 + 计划区间 + 交付物。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneInfo {

    private String name;

    private LocalDate planStart;

    private LocalDate planEnd;

    private String deliverable;

    public boolean hasPlanDate() {
        return planStart != null && planEnd != null;
    }
}

package com.taiji.fund.analysis.run;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 项目分回写指令：最新运行的最高分与风险等级。
 */
@Getter
@AllArgsConstructor
public class ProjectScoreUpdate {

    private final Long projectId;

    private final BigDecimal maxScore;

    private final String level;
}

package com.taiji.fund.analysis.score;

import com.taiji.fund.analysis.support.ProjectAnalysisInput;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 归因判定：
 * <ul>
 *   <li>同组 → SELF_GROUP</li>
 *   <li>跨组且时间差 ≤ 同期窗口 → SIMULTANEOUS</li>
 *   <li>跨组且超出窗口 → 较早方方向为 A_EARLIER / B_EARLIER（A = 第一个参数）</li>
 * </ul>
 */
public class AttributionResolver {

    public Attribution resolve(ProjectAnalysisInput first, ProjectAnalysisInput second, int simultaneousDays) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("归因项目不能为空");
        }
        boolean sameGroup = first.getGroupId() != null && first.getGroupId().equals(second.getGroupId());
        if (sameGroup) {
            return Attribution.builder()
                    .relation(Attribution.RELATION_SELF_GROUP)
                    .direction(Attribution.DIRECTION_SELF_GROUP)
                    .earlierProjectId(earlierByTime(first, second))
                    .build();
        }

        LocalDateTime firstTime = first.effectiveTime();
        LocalDateTime secondTime = second.effectiveTime();
        Long earlierId;
        if (firstTime != null && secondTime != null) {
            if (firstTime.isBefore(secondTime)) {
                earlierId = first.getProjectId();
            } else if (secondTime.isBefore(firstTime)) {
                earlierId = second.getProjectId();
            } else {
                // 提交时间完全相等时按 projectId 小者为先，不归为同期
                earlierId = earlierByProjectId(first, second);
                return Attribution.builder()
                        .relation(Attribution.RELATION_CROSS_GROUP)
                        .direction(isFirstEarlier(earlierId, first)
                                ? Attribution.DIRECTION_A_EARLIER : Attribution.DIRECTION_B_EARLIER)
                        .earlierProjectId(earlierId)
                        .build();
            }
            long deltaDays = Math.abs(ChronoUnit.DAYS.between(firstTime, secondTime));
            String direction = deltaDays <= simultaneousDays
                    ? Attribution.DIRECTION_SIMULTANEOUS
                    : (isFirstEarlier(earlierId, first) ? Attribution.DIRECTION_A_EARLIER
                    : Attribution.DIRECTION_B_EARLIER);
            return Attribution.builder()
                    .relation(Attribution.RELATION_CROSS_GROUP)
                    .direction(direction)
                    .earlierProjectId(earlierId)
                    .build();
        }

        earlierId = earlierByTime(first, second);
        return Attribution.builder()
                .relation(Attribution.RELATION_CROSS_GROUP)
                .direction(isFirstEarlier(earlierId, first)
                        ? Attribution.DIRECTION_A_EARLIER : Attribution.DIRECTION_B_EARLIER)
                .earlierProjectId(earlierId)
                .build();
    }

    private Long earlierByTime(ProjectAnalysisInput first, ProjectAnalysisInput second) {
        LocalDateTime firstTime = first.effectiveTime();
        LocalDateTime secondTime = second.effectiveTime();
        if (firstTime != null && secondTime != null) {
            if (firstTime.isBefore(secondTime)) {
                return first.getProjectId();
            }
            if (secondTime.isBefore(firstTime)) {
                return second.getProjectId();
            }
            return earlierByProjectId(first, second);
        }
        if (firstTime != null) {
            return first.getProjectId();
        }
        if (secondTime != null) {
            return second.getProjectId();
        }
        return earlierByProjectId(first, second);
    }

    private Long earlierByProjectId(ProjectAnalysisInput first, ProjectAnalysisInput second) {
        Long firstId = first.getProjectId();
        Long secondId = second.getProjectId();
        if (firstId == null) {
            return secondId;
        }
        if (secondId == null) {
            return firstId;
        }
        return firstId <= secondId ? firstId : secondId;
    }

    private boolean isFirstEarlier(Long earlierId, ProjectAnalysisInput first) {
        return earlierId == null ? true : earlierId.equals(first.getProjectId());
    }
}

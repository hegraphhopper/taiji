package com.taiji.fund.insight.trend;

import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.project.domain.Project;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 快照行：快照 + 项目维度信息（组、方向、基金类型）。
 */
@Data
@AllArgsConstructor
public class SnapshotRow {

    private DuplicateSnapshot snapshot;

    private Project project;

    private Long groupId;

    private String groupName;

    private String techDirectionName;

    public Long projectId() {
        return snapshot.getProjectId();
    }

    public String fundType() {
        return project == null ? null : project.getFundType();
    }

    public Long techDirectionId() {
        return project == null ? null : project.getTechDirectionId();
    }

    public int attributionTotal() {
        return nullSafe(snapshot.getSelfGroupCount())
                + nullSafe(snapshot.getCrossGroupCount());
    }

    public int selfGroup() {
        return nullSafe(snapshot.getSelfGroupCount());
    }

    public int ourEarlier() {
        return nullSafe(snapshot.getOurEarlierCount());
    }

    public int peerEarlier() {
        return nullSafe(snapshot.getPeerEarlierCount());
    }

    public int simultaneous() {
        return nullSafe(snapshot.getSimultaneousCount());
    }

    public int highPair() {
        return nullSafe(snapshot.getHighPairCount());
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}

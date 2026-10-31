package com.taiji.fund.analysis.support;

import com.taiji.fund.analysis.config.FieldWeights;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 单项目分析输入：五维文本 + 节点区间 + 标签集合 + 归因所需时间字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAnalysisInput {

    private Long projectId;

    private String projectCode;

    private String projectName;

    private Long groupId;

    private Long techDirectionId;

    private String techDirectionName;

    private LocalDateTime submitTime;

    private LocalDateTime createdAt;

    private BigDecimal budget;

    private String goalText;

    private String contentText;

    private String keyTechText;

    private String milestoneText;

    private String documentText;

    private List<MilestoneInfo> milestones;

    private Set<String> tags;

    public List<MilestoneInfo> getMilestones() {
        return milestones == null ? Collections.emptyList() : milestones;
    }

    public Set<String> getTags() {
        return tags == null ? Collections.emptySet() : tags;
    }

    public String text(String dimension) {
        if (FieldWeights.GOAL.equals(dimension)) {
            return goalText == null ? "" : goalText;
        }
        if (FieldWeights.CONTENT.equals(dimension)) {
            return contentText == null ? "" : contentText;
        }
        if (FieldWeights.TECH.equals(dimension)) {
            return keyTechText == null ? "" : keyTechText;
        }
        if (FieldWeights.MILESTONE.equals(dimension)) {
            return milestoneText == null ? "" : milestoneText;
        }
        return "";
    }

    public boolean hasText(String dimension) {
        return !text(dimension).trim().isEmpty();
    }

    /**
     * 归因使用的时间：优先 submit_time，缺失回退 created_at。
     */
    public LocalDateTime effectiveTime() {
        return submitTime != null ? submitTime : createdAt;
    }
}

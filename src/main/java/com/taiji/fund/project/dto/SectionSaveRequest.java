package com.taiji.fund.project.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SectionSaveRequest {

    @Valid
    @Size(max = 50, message = "项目目标最多 50 条")
    private List<GoalItem> goals;

    @Valid
    @Size(max = 50, message = "研制内容最多 50 条")
    private List<ContentItem> contents;

    @Valid
    @Size(max = 50, message = "关键技术最多 50 条")
    private List<KeyTechItem> keyTechs;

    @Valid
    @Size(max = 50, message = "节点计划最多 50 条")
    private List<MilestoneItem> milestones;

    @Valid
    @Size(max = 50, message = "项目成员最多 50 条")
    private List<MemberItem> members;

    @Data
    public static class GoalItem {

        @NotBlank(message = "项目目标内容不能为空")
        @Size(max = 2000, message = "单条目标不能超过 2000 字")
        private String content;

        private Integer sort;
    }

    @Data
    public static class ContentItem {

        @NotBlank(message = "研制内容不能为空")
        @Size(max = 4000, message = "单条研制内容不能超过 4000 字")
        private String content;

        private Integer sort;
    }

    @Data
    public static class KeyTechItem {

        @NotBlank(message = "关键技术名称不能为空")
        @Size(max = 200, message = "关键技术名称不能超过 200 字")
        private String name;

        @Size(max = 2000, message = "关键技术说明不能超过 2000 字")
        private String description;

        private String difficulty;

        private Integer sort;
    }

    @Data
    public static class MilestoneItem {

        @NotBlank(message = "节点名称不能为空")
        @Size(max = 200, message = "节点名称不能超过 200 字")
        private String name;

        private LocalDate planStart;

        private LocalDate planEnd;

        @Size(max = 2000, message = "交付物不能超过 2000 字")
        private String deliverable;

        @Min(value = 0, message = "节点权重不能小于 0")
        @Max(value = 100, message = "节点权重不能大于 100")
        private Integer weight;

        private Integer sort;
    }

    @Data
    public static class MemberItem {

        @NotNull(message = "成员用户不能为空")
        private Long userId;

        @NotBlank(message = "成员角色不能为空")
        private String memberRole;

        @NotNull(message = "投入人月不能为空")
        @DecimalMin(value = "0.01", message = "投入人月必须大于 0")
        private BigDecimal workload;
    }
}

package com.taiji.fund.project.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectDetailVO {

    private Long id;
    private String code;
    private String name;
    private String fundType;
    private Long groupId;
    private String groupName;
    private Long ownerId;
    private String ownerName;
    private Long techDirectionId;
    private String techDirectionName;
    private BigDecimal budget;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String summary;
    private String expectedOutcome;
    private BigDecimal duplicateScore;
    private String riskLevel;
    private LocalDateTime submitTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    private List<ProjectGoalVO> goals;
    private List<ProjectContentVO> contents;
    private List<ProjectKeyTechVO> keyTechs;
    private List<ProjectMilestoneVO> milestones;
    private List<ProjectMemberVO> members;
    private List<ProjectApprovalLogVO> approvalLogs;
    private List<ProjectDocumentVO> documents;
}

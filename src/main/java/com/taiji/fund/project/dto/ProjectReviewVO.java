package com.taiji.fund.project.dto;

import com.taiji.fund.project.domain.ProjectReview;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectReviewVO {

    private Long id;

    private Long projectId;

    private Long reviewerId;

    private String reviewerName;

    private Integer round;

    private BigDecimal score;

    private String opinion;

    private String result;

    private LocalDateTime createdAt;

    public static ProjectReviewVO from(ProjectReview review) {
        return ProjectReviewVO.builder()
                .id(review.getId())
                .projectId(review.getProjectId())
                .reviewerId(review.getReviewerId())
                .round(review.getRound())
                .score(review.getScore())
                .opinion(review.getOpinion())
                .result(review.getResult())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

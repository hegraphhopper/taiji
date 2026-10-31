package com.taiji.fund.decision.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 建议评审专家：不返回邮箱、手机号等敏感信息。
 */
@Data
@Builder
public class SuggestedExpertVO {

    private Long id;

    private String realName;

    private String groupName;

    private Integer ongoingReviews;

    private Integer sameDirectionReviews;
}

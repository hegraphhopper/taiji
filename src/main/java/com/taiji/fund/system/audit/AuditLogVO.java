package com.taiji.fund.system.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogVO {

    private Long id;

    private Long userId;

    private String username;

    private String action;

    private String resourceType;

    private String resourceId;

    private String detail;

    private String ip;

    private String result;

    private Long costMs;

    private LocalDateTime createdAt;
}

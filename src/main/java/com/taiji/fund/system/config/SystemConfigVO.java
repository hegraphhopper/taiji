package com.taiji.fund.system.config;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemConfigVO {

    private String key;

    private String value;

    private String description;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}

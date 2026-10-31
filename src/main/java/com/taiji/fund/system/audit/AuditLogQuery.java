package com.taiji.fund.system.audit;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AuditLogQuery {

    private LocalDate from;

    private LocalDate to;

    private String username;

    private String action;

    private String resourceType;

    private String result;

    private Long page = 1L;

    private Long size = 10L;
}

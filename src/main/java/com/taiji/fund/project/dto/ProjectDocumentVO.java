package com.taiji.fund.project.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectDocumentVO {

    private Long id;
    private Long projectId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String parseStatus;
    private String parseMessage;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}

package com.taiji.fund.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_document")
public class ProjectDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String fileName;

    private String storedName;

    private String fileType;

    private Long fileSize;

    private String storagePath;

    private String textContent;

    private String parseStatus;

    private String parseMessage;

    private Long uploadedBy;

    private LocalDateTime createdAt;
}

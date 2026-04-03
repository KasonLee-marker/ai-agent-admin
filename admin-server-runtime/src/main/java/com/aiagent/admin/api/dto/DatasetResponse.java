package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DatasetResponse {
    private String id;
    private String name;
    private String description;
    private String category;
    private String tags;
    private Integer version;
    private String status;
    private Integer itemCount;
    private String sourceType;
    private String sourcePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}

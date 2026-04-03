package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DatasetItemResponse {
    private String id;
    private String datasetId;
    private Integer version;
    private Integer sequence;
    private String input;
    private String output;
    private String metadata;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.aiagent.admin.prompt.api.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromptTemplateResponse {
    private String id;
    private String name;
    private String content;
    private String description;
    private String category;
    private List<String> tags;
    private Integer version;
    private List<String> variables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}

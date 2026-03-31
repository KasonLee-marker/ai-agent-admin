package com.aiagent.admin.prompt.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromptVersionResponse {
    private String id;
    private String promptId;
    private Integer version;
    private String content;
    private String changeLog;
    private LocalDateTime createdAt;
}

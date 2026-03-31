package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RollbackRequest {
    @NotNull(message = "Version is required")
    private Integer version;

    private String changeLog;
}

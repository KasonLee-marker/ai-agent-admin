package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 状态更新请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatusRequest {

    /**
     * 目标状态
     * <p>
     * DRAFT | PUBLISHED | ARCHIVED
     * </p>
     */
    @NotBlank(message = "Status is required")
    private String status;
}
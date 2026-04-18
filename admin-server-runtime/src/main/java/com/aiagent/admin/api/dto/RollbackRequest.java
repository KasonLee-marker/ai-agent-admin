package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提示词版本回滚请求 DTO
 * <p>
 * 用于将提示词模板回滚到指定的历史版本。
 * </p>
 */
@Data
public class RollbackRequest {

    /**
     * 目标版本号（必填）
     */
    @NotNull(message = "Version is required")
    private Integer version;

    /** 变更日志（可选） */
    private String changeLog;
}

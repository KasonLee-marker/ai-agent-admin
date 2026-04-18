package com.aiagent.admin.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词版本响应 DTO
 * <p>
 * 返回提示词模板的历史版本信息，用于版本管理和回滚。
 * </p>
 */
@Data
public class PromptVersionResponse {

    /**
     * 版本 ID
     */
    private String id;

    /** 所属模板 ID */
    private String promptId;

    /** 版本号 */
    private Integer version;

    /** 版本内容 */
    private String content;

    /** 变更日志 */
    private String changeLog;

    /** 创建时间 */
    private LocalDateTime createdAt;
}

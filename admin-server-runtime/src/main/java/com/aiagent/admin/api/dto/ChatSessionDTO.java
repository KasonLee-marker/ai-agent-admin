package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天会话 DTO
 * <p>
 * 表示一个聊天会话的基本信息，包括标题、模型配置、消息数量等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDTO {

    /**
     * 会话 ID
     */
    private String id;

    /** 会话标题 */
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    /** 默认模型配置 ID */
    private String modelId;

    /** 关联的提示词模板 ID */
    private String promptId;

    /** 系统消息 */
    @Size(max = 500, message = "System message must not exceed 500 characters")
    private String systemMessage;

    /** 消息数量 */
    private Integer messageCount;

    /** 是否活跃 */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 创建者 */
    private String createdBy;
}

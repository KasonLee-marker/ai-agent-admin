package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.MessageRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 消息响应 DTO
 * <p>
 * 返回 RAG 会话中的单条消息，包含角色、内容、检索来源等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG消息响应")
public class RagMessageDTO {

    /**
     * 消息 ID
     */
    @Schema(description = "消息ID")
    private String id;

    /** 所属会话 ID */
    @Schema(description = "会话ID")
    private String sessionId;

    /** 消息角色（USER/ASSISTANT） */
    @Schema(description = "消息角色（USER/ASSISTANT）")
    private MessageRole role;

    /** 消息内容 */
    @Schema(description = "消息内容")
    private String content;

    /** 检索来源（仅助手消息，包含引用的文档片段） */
    @Schema(description = "检索来源（仅助手消息）")
    private List<VectorSearchResult> sources;

    /** 使用的模型名称 */
    @Schema(description = "模型名称")
    private String modelName;

    /** 响应延迟（毫秒） */
    @Schema(description = "响应延迟（毫秒）")
    private Long latencyMs;

    /** 是否错误消息 */
    @Schema(description = "是否错误消息")
    private Boolean isError;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
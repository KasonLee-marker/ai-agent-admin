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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG消息响应")
public class RagMessageDTO {

    @Schema(description = "消息ID")
    private String id;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "消息角色（USER/ASSISTANT）")
    private MessageRole role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "检索来源（仅助手消息）")
    private List<VectorSearchResult> sources;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "响应延迟（毫秒）")
    private Long latencyMs;

    @Schema(description = "是否错误消息")
    private Boolean isError;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
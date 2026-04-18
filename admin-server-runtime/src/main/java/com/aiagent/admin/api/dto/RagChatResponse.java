package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG对话响应 DTO
 * <p>
 * 返回 RAG 对话的回答、引用来源和会话信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG对话响应")
public class RagChatResponse {

    /**
     * AI 回答内容
     */
    @Schema(description = "AI回答")
    private String answer;

    /** 引用的文档片段列表 */
    @Schema(description = "引用的文档片段")
    private List<VectorSearchResult> sources;

    /** 会话 ID（用于多轮对话） */
    @Schema(description = "会话ID（用于多轮对话）")
    private String sessionId;

    /** 响应延迟（毫秒） */
    @Schema(description = "响应延迟（毫秒）")
    private Long latencyMs;

    /** 使用的模型名称 */
    @Schema(description = "使用的模型")
    private String modelName;
}
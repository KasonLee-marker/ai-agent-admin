package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG对话响应")
public class RagChatResponse {

    @Schema(description = "AI回答")
    private String answer;

    @Schema(description = "引用的文档片段")
    private List<VectorSearchResult> sources;

    @Schema(description = "响应延迟（毫秒）")
    private Long latencyMs;

    @Schema(description = "使用的模型")
    private String modelName;
}
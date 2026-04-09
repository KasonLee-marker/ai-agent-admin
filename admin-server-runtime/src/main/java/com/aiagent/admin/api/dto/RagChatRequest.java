package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "RAG对话请求")
public class RagChatRequest {

    @Schema(description = "用户问题")
    @NotBlank(message = "问题不能为空")
    private String question;

    @Schema(description = "检索结果数量", defaultValue = "5")
    @Min(value = 1, message = "topK最小为1")
    @Max(value = 20, message = "topK最大为20")
    private Integer topK = 5;

    @Schema(description = "模型配置ID（可选）")
    private String modelId;

    @Schema(description = "文档ID过滤（可选）")
    private String documentId;

    @Schema(description = "系统提示词模板（可选）")
    private String systemPromptTemplate;
}
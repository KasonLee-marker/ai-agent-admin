package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "向量搜索请求")
public class VectorSearchRequest {

    @Schema(description = "搜索查询文本")
    @NotBlank(message = "查询文本不能为空")
    private String query;

    @Schema(description = "返回结果数量", defaultValue = "5")
    @Min(value = 1, message = "topK最小为1")
    @Max(value = 100, message = "topK最大为100")
    private Integer topK = 5;

    @Schema(description = "相似度阈值 (0-1)", defaultValue = "0.7")
    @Min(value = 0, message = "阈值最小为0")
    @Max(value = 1, message = "阈值最大为1")
    private Double threshold = 0.7;

    @Schema(description = "文档ID过滤（可选）")
    private String documentId;

    @Schema(description = "Embedding模型ID（可选，默认使用系统默认embedding模型）")
    private String embeddingModelId;
}
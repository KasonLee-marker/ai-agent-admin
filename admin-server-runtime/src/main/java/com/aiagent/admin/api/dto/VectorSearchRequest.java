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

    /**
     * 知识库 ID 过滤
     * <p>
     * 仅检索指定知识库下的文档。
     * </p>
     */
    @Schema(description = "知识库ID过滤（可选）")
    private String knowledgeBaseId;

    /**
     * 检索策略
     * <p>
     * 支持：VECTOR（向量检索）、BM25（关键词检索）、HYBRID（混合检索）。
     * </p>
     */
    @Schema(description = "检索策略（VECTOR/BM25/HYBRID）", defaultValue = "VECTOR")
    private String strategy;

    /**
     * 是否启用 Rerank 重排序
     * <p>
     * 启用后，先获取候选结果（topK * 4），再调用 Rerank API 进行二次排序，
     * 返回最终 topK 个结果。
     * </p>
     */
    @Schema(description = "是否启用 Rerank 重排序", defaultValue = "false")
    private Boolean enableRerank = false;

    /**
     * Rerank 模型 ID
     * <p>
     * 当 enableRerank=true 时使用，指定用于重排序的模型配置。
     * </p>
     */
    @Schema(description = "Rerank 模型 ID（当 enableRerank=true 时使用）")
    private String rerankModelId;
}
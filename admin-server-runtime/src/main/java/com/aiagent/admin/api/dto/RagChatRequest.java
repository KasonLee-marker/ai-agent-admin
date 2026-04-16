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

    @Schema(description = "提示词模板ID（可选，优先级高于 systemPromptTemplate）")
    private String promptTemplateId;

    @Schema(description = "检索结果数量", defaultValue = "5")
    @Min(value = 1, message = "topK最小为1")
    @Max(value = 20, message = "topK最大为20")
    private Integer topK = 5;

    @Schema(description = "模型配置ID（可选）")
    private String modelId;

    @Schema(description = "Embedding模型ID（可选，用于向量检索，默认使用系统默认embedding模型）")
    private String embeddingModelId;

    @Schema(description = "文档ID过滤（可选）")
    private String documentId;

    /**
     * 相似度阈值
     * <p>
     * 检索结果的相似度必须大于此阈值才会返回。
     * </p>
     */
    @Schema(description = "相似度阈值 (0-1)", defaultValue = "0.5")
    @Min(value = 0, message = "阈值最小为0")
    @Max(value = 1, message = "阈值最大为1")
    private Double threshold;

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
     * BM25 和 HYBRID 为 P2 功能，当前版本仅支持 VECTOR。
     * </p>
     */
    @Schema(description = "检索策略（VECTOR/BM25/HYBRID）", defaultValue = "VECTOR")
    private String strategy;

    /**
     * RAG 会话 ID
     * <p>
     * 用于多轮对话。如果为空，将自动创建新会话。
     * </p>
     */
    @Schema(description = "RAG会话ID（可选，用于多轮对话）")
    private String sessionId;

    @Schema(description = "系统提示词模板（可选，promptTemplateId 存在时忽略）")
    private String systemPromptTemplate;

    /**
     * 是否启用 Rerank 重排序
     * <p>
     * 启用后，先获取候选结果（topK * 4），再调用 Rerank API 进行二次排序，
     * 返回最终 topK 个结果，提高检索精度。
     * </p>
     */
    @Schema(description = "是否启用 Rerank 重排序", defaultValue = "false")
    private Boolean enableRerank = false;

    /**
     * Rerank 模型 ID
     * <p>
     * 当 enableRerank=true 时使用，指定用于重排序的模型配置。
     * 支持 Cohere Rerank 和 Jina Rerank。
     * </p>
     */
    @Schema(description = "Rerank 模型 ID（当 enableRerank=true 时使用）")
    private String rerankModelId;
}
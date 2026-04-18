package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档响应 DTO
 * <p>
 * 返回文档的详细信息，包括分块状态、Embedding 信息、语义切分进度等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档响应")
public class DocumentResponse {

    /**
     * 文档 ID
     */
    @Schema(description = "文档ID")
    private String id;

    /** 文档名称 */
    @Schema(description = "文档名称")
    private String name;

    /** 内容类型（MIME） */
    @Schema(description = "内容类型")
    private String contentType;

    /** 文件大小（字节） */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /** 总分块数 */
    @Schema(description = "总分块数")
    private Integer totalChunks;

    /** 分块策略（FIXED/RECURSIVE/SEMANTIC 等） */
    @Schema(description = "分块策略")
    private String chunkStrategy;

    /** 分块大小 */
    @Schema(description = "分块大小")
    private Integer chunkSize;

    /** 分块重叠大小 */
    @Schema(description = "分块重叠")
    private Integer chunkOverlap;

    /** 已创建分块数 */
    @Schema(description = "已创建分块数")
    private Integer chunksCreated;

    /** 已 Embedding 分块数 */
    @Schema(description = "已Embedding分块数")
    private Integer chunksEmbedded;

    /** Embedding 模型配置 ID */
    @Schema(description = "Embedding模型ID")
    private String embeddingModelId;

    /** Embedding 模型名称 */
    @Schema(description = "Embedding模型名称")
    private String embeddingModelName;

    /** 向量维度 */
    @Schema(description = "向量维度")
    private Integer embeddingDimension;

    /**
     * 所属知识库 ID
     */
    @Schema(description = "知识库ID")
    private String knowledgeBaseId;

    /**
     * 所属知识库名称
     */
    @Schema(description = "知识库名称")
    private String knowledgeBaseName;

    /** 文档处理状态 */
    @Schema(description = "状态")
    private Document.DocumentStatus status;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 创建者 */
    @Schema(description = "创建人")
    private String createdBy;

    /**
     * 语义切分进度 - 已处理句子数
     * <p>
     * 仅在 SEMANTIC 分块策略时使用，表示已计算 Embedding 的句子数量。
     * </p>
     */
    @Schema(description = "语义切分已处理句子数")
    private Integer semanticProgressCurrent;

    /**
     * 语义切分进度 - 总句子数
     * <p>
     * 仅在 SEMANTIC 分块策略时使用，表示文档的总句子数量。
     * </p>
     */
    @Schema(description = "语义切分总句子数")
    private Integer semanticProgressTotal;
}
package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库创建/更新请求 DTO
 * <p>
 * 用于创建或更新知识库的请求参数。
 * 创建知识库时必须指定默认 Embedding 模型，所有上传文档将使用该模型进行向量索引。
 * 如果更新时更改了 Embedding 模型，将触发全库重索引。
 * </p>
 */
@Data
@Schema(description = "知识库请求")
public class KnowledgeBaseRequest {

    /**
     * 知识库名称
     */
    @Schema(description = "知识库名称")
    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称最长100字符")
    private String name;

    /**
     * 知识库描述
     */
    @Schema(description = "知识库描述")
    @Size(max = 500, message = "描述最长500字符")
    private String description;

    /**
     * 默认 Embedding 模型 ID（必填）
     * <p>
     * 所有上传到该知识库的文档将使用此模型进行向量索引。
     * 如果更改此模型，将触发全库重索引。
     * </p>
     */
    @Schema(description = "默认 Embedding 模型 ID（必填）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Embedding 模型不能为空，请选择一个 Embedding 模型")
    private String defaultEmbeddingModelId;
}
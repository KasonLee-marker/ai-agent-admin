package com.aiagent.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 重索引请求
 * <p>
 * 用于启动知识库 Embedding 模型切换后的批量重计算。
 * </p>
 */
@Data
@Schema(description = "重索引请求")
public class ReindexRequest {

    @Schema(description = "新 Embedding 模型 ID")
    private String newEmbeddingModelId;
}
package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.entity.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语义切分进度响应 DTO
 * <p>
 * 返回语义切分处理的实时进度信息，用于前端轮询显示。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "语义切分进度响应")
public class SemanticProgressResponse {

    /**
     * 文档当前状态
     */
    @Schema(description = "文档状态")
    private Document.DocumentStatus status;

    /**
     * 已处理的句子数
     * <p>
     * 表示已计算 Embedding 的句子数量。
     * </p>
     */
    @Schema(description = "已处理句子数")
    private Integer current;

    /**
     * 总句子数
     * <p>
     * 表示文档的总句子数量，用于计算进度百分比。
     * </p>
     */
    @Schema(description = "总句子数")
    private Integer total;

    /**
     * 进度百分比（0-100）
     * <p>
     * 计算公式：current / total * 100
     * </p>
     */
    @Schema(description = "进度百分比")
    private Integer percentage;

    /**
     * 错误信息（处理失败时）
     */
    @Schema(description = "错误信息")
    private String errorMessage;
}
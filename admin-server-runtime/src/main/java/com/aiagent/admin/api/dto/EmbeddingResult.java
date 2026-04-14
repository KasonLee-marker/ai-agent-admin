package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedding 计算结果
 * <p>
 * 包含向量数据和维度信息，便于后续存储到正确的向量表。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResult {

    /**
     * 向量数据
     */
    private float[] vector;

    /**
     * 向量维度
     */
    private int dimension;

    /**
     * 目标存储表名
     */
    private String tableName;
}
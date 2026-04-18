package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 评估任务对比请求 DTO
 * <p>
 * 用于对比两个评估任务的结果，分析性能差异。
 * </p>
 */
@Data
public class EvaluationCompareRequest {

    /**
     * 第一个评估任务 ID（必填）
     */
    @NotBlank(message = "First job ID is required")
    private String jobId1;

    /** 第二个评估任务 ID（必填） */
    @NotBlank(message = "Second job ID is required")
    private String jobId2;
}

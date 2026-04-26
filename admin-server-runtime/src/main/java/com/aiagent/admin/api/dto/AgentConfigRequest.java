package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 运行配置请求 DTO
 * <p>
 * 包含 temperature、maxTokens 等运行参数。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigRequest {

    /**
     * 输出随机性参数（0.0 - 1.0）
     */
    private Double temperature;

    /**
     * 最大输出 Token 数
     */
    private Integer maxTokens;

    /**
     * Top P 参数
     */
    private Double topP;
}
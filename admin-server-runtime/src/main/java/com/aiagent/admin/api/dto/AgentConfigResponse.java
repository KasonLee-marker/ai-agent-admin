package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 配置响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigResponse {

    /**
     * 输出随机性参数
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
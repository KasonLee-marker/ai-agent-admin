package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 响应 DTO
 * <p>
 * 返回 Agent 的完整信息，包含绑定工具列表。
 * </p>
 *
 * @see ToolBindingResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    /**
     * Agent ID
     */
    private String id;

    /**
     * Agent 名称
     */
    private String name;

    /**
     * Agent 描述
     */
    private String description;

    /**
     * Agent 版本号
     */
    private String version;

    /**
     * 绑定的模型 ID
     */
    private String modelId;

    /**
     * 绑定的模型名称（用于显示）
     */
    private String modelName;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * Agent 配置
     */
    private AgentConfigResponse config;

    /**
     * Agent 状态
     */
    private AgentStatus status;

    /**
     * 绑定的工具列表
     */
    private List<ToolBindingResponse> tools;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
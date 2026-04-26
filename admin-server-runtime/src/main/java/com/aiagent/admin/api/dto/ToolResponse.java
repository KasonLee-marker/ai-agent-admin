package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Tool 响应 DTO
 * <p>
 * 返回工具的完整信息，包含 JSON Schema 和配置。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResponse {

    /**
     * 工具 ID
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具类别
     */
    private ToolCategory category;

    /**
     * 工具类型
     */
    private ToolType type;

    /**
     * JSON Schema（输入参数定义）
     */
    private Map<String, Object> schema;

    /**
     * 执行器标识
     */
    private String executor;

    /**
     * 工具配置
     */
    private Map<String, Object> config;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
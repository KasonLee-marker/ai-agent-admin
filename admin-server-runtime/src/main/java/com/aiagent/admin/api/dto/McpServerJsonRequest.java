package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server JSON 配置请求
 * <p>
 * 用户输入 MCP 配置 JSON，格式如：
 * <pre>
 * {
 *   "mcpServers": {
 *     "server-name": {
 *       "url": "https://..."
 *     }
 *   }
 * }
 * </pre>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerJsonRequest {

    /**
     * MCP Server 配置 JSON
     * <p>
     * 格式：{"mcpServers": {"name": {"url": "..."} 或 {"command": "...", "args": []}}}
     * </p>
     */
    @NotBlank(message = "配置 JSON 不能为空")
    private String configJson;
}
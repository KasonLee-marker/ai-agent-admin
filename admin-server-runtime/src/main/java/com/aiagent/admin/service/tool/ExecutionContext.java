package com.aiagent.admin.service.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具执行上下文
 * <p>
 * 包含工具执行所需的环境信息，如 Agent ID、工具绑定配置等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 会话 ID（可选）
     */
    private String sessionId;

    /**
     * Agent-Tool 绑定配置
     * <p>
     * 从 agent_tools.config 读取，如 knowledge_retrieval 的 kbId、topK 等。
     * </p>
     */
    private Map<String, Object> agentToolConfig;

    /**
     * 用户传入的执行上下文（可选）
     */
    private Map<String, Object> userContext;
}
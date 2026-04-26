package com.aiagent.admin.service.tool;

import java.util.Map;

/**
 * 工具执行器接口
 * <p>
 * 定义工具执行的标准接口，所有内置工具执行器必须实现此接口。
 * </p>
 */
public interface ToolExecutor {

    /**
     * 获取执行器名称
     * <p>
     * 对应 Tool 实体的 executor 字段。
     * </p>
     *
     * @return 执行器名称
     */
    String getName();

    /**
     * 执行工具
     *
     * @param args    工具参数（从 LLM tool_calls 解析）
     * @param context 执行上下文（包含 agentId、agentToolConfig 等）
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> args, ExecutionContext context);

    /**
     * 获取工具的 JSON Schema
     * <p>
     * 用于注册工具时生成 schema 字段。
     * </p>
     *
     * @return JSON Schema 字符串
     */
    String getSchema();
}
package com.aiagent.admin.service.tool;

import com.aiagent.admin.domain.entity.Tool;
import com.aiagent.admin.domain.enums.ToolCategory;
import com.aiagent.admin.domain.enums.ToolType;
import com.aiagent.admin.domain.repository.ToolRepository;
import com.aiagent.admin.service.ToolIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置工具初始化器
 * <p>
 * 应用启动时自动注册内置工具到数据库。
 * 内置工具包括：
 * <ul>
 *   <li>calculator - 数学计算工具</li>
 *   <li>datetime - 日期时间工具</li>
 *   <li>knowledge_retrieval - 知识库检索工具</li>
 * </ul>
 * </p>
 *
 * @see ToolExecutor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltInToolInitializer implements ApplicationRunner {

    private final ToolRepository toolRepository;
    private final ToolIdGenerator toolIdGenerator;
    private final List<ToolExecutor> toolExecutors;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing built-in tools...");

        for (ToolExecutor executor : toolExecutors) {
            registerTool(executor);
        }

        log.info("Built-in tools initialization completed. Registered {} tools.", toolExecutors.size());
    }

    /**
     * 注册内置工具
     *
     * @param executor 工具执行器
     */
    private void registerTool(ToolExecutor executor) {
        String toolName = getToolNameFromExecutor(executor);

        // 检查是否已存在
        if (toolRepository.existsByName(toolName)) {
            log.debug("Built-in tool '{}' already exists, skipping registration", toolName);
            return;
        }

        // 创建工具实体
        Tool tool = Tool.builder()
                .id(toolIdGenerator.generateId())
                .name(toolName)
                .description(getDescription(toolName))
                .category(getCategory(toolName))
                .type(ToolType.BUILTIN)
                .schema(executor.getSchema())
                .executor(executor.getName())
                .config("{}")
                .build();

        toolRepository.save(tool);
        log.info("Registered built-in tool: {} (executor: {})", toolName, executor.getName());
    }

    /**
     * 从执行器名称推断工具名称
     */
    private String getToolNameFromExecutor(ToolExecutor executor) {
        String executorName = executor.getName();
        // executorName 格式如 calculatorExecutor -> toolName calculator
        if (executorName.endsWith("Executor")) {
            return executorName.substring(0, executorName.length() - "Executor".length());
        }
        return executorName;
    }

    /**
     * 获取工具描述
     */
    private String getDescription(String toolName) {
        return switch (toolName) {
            case "calculator" ->
                    "Calculate mathematical expressions. Input: expression string. Output: numeric result.";
            case "datetime" ->
                    "Get current date and time. Input: optional timezone. Output: formatted datetime string.";
            case "knowledgeRetrieval" ->
                    "Retrieve relevant documents from knowledge base. Input: query string. Output: document list with scores.";
            default -> "Built-in tool: " + toolName;
        };
    }

    /**
     * 获取工具类别
     */
    private ToolCategory getCategory(String toolName) {
        return switch (toolName) {
            case "calculator" -> ToolCategory.CALCULATION;
            case "knowledgeRetrieval" -> ToolCategory.KNOWLEDGE;
            default -> ToolCategory.GENERAL;
        };
    }
}
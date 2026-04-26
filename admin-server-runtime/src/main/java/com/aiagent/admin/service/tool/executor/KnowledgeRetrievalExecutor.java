package com.aiagent.admin.service.tool.executor;

import com.aiagent.admin.api.dto.RagChatRequest;
import com.aiagent.admin.api.dto.VectorSearchResult;
import com.aiagent.admin.service.RagService;
import com.aiagent.admin.service.tool.ExecutionContext;
import com.aiagent.admin.service.tool.ToolExecutor;
import com.aiagent.admin.service.tool.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库检索工具执行器
 * <p>
 * 从知识库检索相关文档，返回检索结果供 LLM 参考。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetrievalExecutor implements ToolExecutor {

    private final RagService ragService;

    @Override
    public String getName() {
        return "knowledgeRetrievalExecutor";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取查询文本
            String query = (String) args.get("query");
            if (query == null || query.isEmpty()) {
                return ToolResult.failure("Query is required");
            }

            // 从 agentToolConfig 获取知识库配置
            Map<String, Object> config = context.getAgentToolConfig();
            String kbId = getStringConfig(config, "kbId");
            if (kbId == null || kbId.isEmpty()) {
                return ToolResult.failure("Knowledge base ID (kbId) is required in tool config");
            }

            Integer topK = getIntConfig(config, "topK", 5);
            Double threshold = getDoubleConfig(config, "threshold", 0.3);
            String strategy = getStringConfig(config, "strategy", "VECTOR");

            // 构建检索请求
            RagChatRequest request = new RagChatRequest();
            request.setQuestion(query);
            request.setKnowledgeBaseId(kbId);
            request.setTopK(topK);
            request.setThreshold(threshold);
            request.setStrategy(strategy);

            // 执行检索
            List<VectorSearchResult> results = ragService.retrieve(request);

            // 转换结果
            List<Map<String, Object>> documents = results.stream()
                    .map(result -> Map.<String, Object>of(
                            "documentId", result.getDocumentId() != null ? result.getDocumentId() : "",
                            "content", result.getContent() != null ? result.getContent() : "",
                            "score", result.getScore() != null ? result.getScore() : 0.0
                    ))
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Knowledge retrieval executed: kbId={}, query={}, results={} ({}ms)",
                    kbId, query, documents.size(), duration);

            return ToolResult.success(Map.of(
                    "query", query,
                    "knowledgeBaseId", kbId,
                    "documents", documents,
                    "total", documents.size()
            ), duration);

        } catch (Exception e) {
            log.error("Knowledge retrieval execution failed: {}", e.getMessage(), e);
            return ToolResult.failure("Knowledge retrieval failed: " + e.getMessage());
        }
    }

    @Override
    public String getSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string",
                      "description": "Search query text for knowledge retrieval"
                    }
                  },
                  "required": ["query"]
                }
                """;
    }

    /**
     * 从配置获取字符串值
     */
    private String getStringConfig(Map<String, Object> config, String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从配置获取字符串值（带默认值）
     */
    private String getStringConfig(Map<String, Object> config, String key, String defaultValue) {
        String value = getStringConfig(config, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 从配置获取整数值
     */
    private Integer getIntConfig(Map<String, Object> config, String key, Integer defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 从配置获取浮点数值
     */
    private Double getDoubleConfig(Map<String, Object> config, String key, Double defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
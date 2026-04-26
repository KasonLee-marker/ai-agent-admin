package com.aiagent.admin.service.tool.executor;

import com.aiagent.admin.service.tool.ExecutionContext;
import com.aiagent.admin.service.tool.ToolExecutor;
import com.aiagent.admin.service.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 日期时间工具执行器
 * <p>
 * 获取当前日期和时间，支持指定时区。
 * </p>
 */
@Slf4j
@Component
public class DatetimeExecutor implements ToolExecutor {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getName() {
        return "datetimeExecutor";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取时区参数（可选）
            String timezone = args.containsKey("timezone") ? (String) args.get("timezone") : "UTC";

            // 获取当前时间
            ZoneId zoneId = parseZoneId(timezone);
            LocalDateTime now = LocalDateTime.now(zoneId);
            String formatted = now.format(DEFAULT_FORMATTER);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Datetime executed: timezone={}, result={} ({}ms)", timezone, formatted, duration);

            return ToolResult.success(Map.of(
                    "datetime", formatted,
                    "timezone", timezone,
                    "epoch", System.currentTimeMillis()
            ), duration);

        } catch (Exception e) {
            log.error("Datetime execution failed: {}", e.getMessage());
            return ToolResult.failure("Failed to get datetime: " + e.getMessage());
        }
    }

    @Override
    public String getSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "timezone": {
                      "type": "string",
                      "description": "Timezone identifier, e.g., 'UTC', 'Asia/Shanghai', 'America/New_York'"
                    }
                  },
                  "required": []
                }
                """;
    }

    /**
     * 解析时区 ID
     *
     * @param timezone 时区字符串
     * @return ZoneId
     */
    private ZoneId parseZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            // 默认使用 UTC
            return ZoneId.of("UTC");
        }
    }
}
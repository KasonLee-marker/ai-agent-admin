package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String id;

    private String sessionId;

    private MessageRole role;

    private String content;

    private String modelName;

    private Integer tokenCount;

    private Long latencyMs;

    private Boolean isError;

    private String errorMessage;

    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionListResponse {
        private List<ChatSessionDTO> sessions;
        private long total;
        private int page;
        private int size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageListResponse {
        private String sessionId;
        private List<ChatResponse> messages;
        private long total;
    }
}

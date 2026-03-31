package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    private String modelId;

    private Boolean stream;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSessionRequest {
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        private String title;

        private String modelId;

        private String promptId;

        @Size(max = 500, message = "System message must not exceed 500 characters")
        private String systemMessage;
    }
}

package com.aiagent.admin.api.dto;

import lombok.Data;

@Data
public class PromptListRequest {
    private String category;
    private String tag;
    private String keyword;
    private Integer page = 0;
    private Integer size = 20;
}

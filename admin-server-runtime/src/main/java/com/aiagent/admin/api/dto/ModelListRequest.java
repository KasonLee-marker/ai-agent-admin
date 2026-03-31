package com.aiagent.admin.api.dto;

import lombok.Data;

@Data
public class ModelListRequest {

    private String provider;
    private Boolean isActive;
    private String keyword;
}

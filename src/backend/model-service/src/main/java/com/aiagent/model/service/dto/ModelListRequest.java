package com.aiagent.model.service.dto;

import lombok.Data;

@Data
public class ModelListRequest {

    private String provider;
    private Boolean isActive;
    private String keyword;
}

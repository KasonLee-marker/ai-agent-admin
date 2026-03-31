package com.aiagent.model.service.dto;

import com.aiagent.model.domain.entity.ModelProvider;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProviderResponse {

    private String name;
    private String displayName;
    private String defaultBaseUrl;
    private List<ModelProvider.BuiltinModel> builtinModels;
}

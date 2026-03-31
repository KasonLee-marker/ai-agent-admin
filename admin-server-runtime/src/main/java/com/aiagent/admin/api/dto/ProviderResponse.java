package com.aiagent.admin.api.dto;

import com.aiagent.admin.domain.enums.ModelProvider;
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

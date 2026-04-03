package com.aiagent.admin.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EvaluationJobUpdateRequest {

    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;
}

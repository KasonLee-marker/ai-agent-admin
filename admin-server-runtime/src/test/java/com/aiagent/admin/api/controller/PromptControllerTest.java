package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.PromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PromptController.class)
@ContextConfiguration(classes = PromptController.class)
class PromptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptService promptService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPrompt_Success() throws Exception {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setName("Test Prompt");
        request.setContent("Hello {{name}}!");

        PromptTemplateResponse response = new PromptTemplateResponse();
        response.setId("test-id");
        response.setName("Test Prompt");

        when(promptService.createPrompt(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("test-id"));
    }

    @Test
    void createPrompt_ValidationError() throws Exception {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setName("");
        request.setContent("");

        mockMvc.perform(post("/api/v1/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPrompt_Success() throws Exception {
        PromptTemplateResponse response = new PromptTemplateResponse();
        response.setId("test-id");
        response.setName("Test Prompt");

        when(promptService.getPrompt("test-id")).thenReturn(response);

        mockMvc.perform(get("/api/v1/prompts/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("test-id"));
    }

    @Test
    void listPrompts_Success() throws Exception {
        PageResponse<PromptTemplateResponse> pageResponse = PageResponse.<PromptTemplateResponse>builder()
                .content(Collections.singletonList(new PromptTemplateResponse()))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(promptService.listPrompts(any(), any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/prompts")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void updatePrompt_Success() throws Exception {
        PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
        request.setName("Updated Prompt");
        request.setContent("Updated content");

        PromptTemplateResponse response = new PromptTemplateResponse();
        response.setId("test-id");
        response.setName("Updated Prompt");

        when(promptService.updatePrompt(eq("test-id"), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/prompts/test-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deletePrompt_Success() throws Exception {
        doNothing().when(promptService).deletePrompt("test-id");

        mockMvc.perform(delete("/api/v1/prompts/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getPromptVersions_Success() throws Exception {
        when(promptService.getPromptVersions("test-id"))
                .thenReturn(Arrays.asList(new PromptVersionResponse()));

        mockMvc.perform(get("/api/v1/prompts/test-id/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void rollbackPrompt_Success() throws Exception {
        RollbackRequest request = new RollbackRequest();
        request.setVersion(1);

        PromptTemplateResponse response = new PromptTemplateResponse();
        response.setId("test-id");
        response.setVersion(2);

        when(promptService.rollbackPrompt(eq("test-id"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/prompts/test-id/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

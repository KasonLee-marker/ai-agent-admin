package com.aiagent.admin.api.controller;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.service.EvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EvaluationController.class)
@ContextConfiguration(classes = EvaluationController.class)
class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationService evaluationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createJob_Success() throws Exception {
        EvaluationJobCreateRequest request = new EvaluationJobCreateRequest();
        request.setName("Test Job");
        request.setPromptTemplateId("prompt-123");
        request.setModelConfigId("model-123");
        request.setDatasetId("dataset-123");

        EvaluationJobResponse response = new EvaluationJobResponse();
        response.setId("job-123");
        response.setName("Test Job");
        response.setStatus("PENDING");

        when(evaluationService.createJob(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("job-123"));
    }

    @Test
    void getJob_Success() throws Exception {
        EvaluationJobResponse response = new EvaluationJobResponse();
        response.setId("job-123");
        response.setName("Test Job");
        response.setStatus("PENDING");

        when(evaluationService.getJob("job-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/evaluations/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("job-123"));
    }

    @Test
    void listJobs_Success() throws Exception {
        PageResponse<EvaluationJobResponse> pageResponse = PageResponse.of(
                Collections.singletonList(new EvaluationJobResponse()), 0, 20, 1);

        when(evaluationService.listJobs(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void updateJob_Success() throws Exception {
        EvaluationJobUpdateRequest request = new EvaluationJobUpdateRequest();
        request.setName("Updated Job");

        EvaluationJobResponse response = new EvaluationJobResponse();
        response.setId("job-123");
        response.setName("Updated Job");

        when(evaluationService.updateJob(eq("job-123"), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/evaluations/job-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteJob_Success() throws Exception {
        doNothing().when(evaluationService).deleteJob("job-123");

        mockMvc.perform(delete("/api/v1/evaluations/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void runJob_Success() throws Exception {
        EvaluationJobResponse response = new EvaluationJobResponse();
        response.setId("job-123");
        response.setStatus("RUNNING");

        when(evaluationService.runJob("job-123")).thenReturn(CompletableFuture.completedFuture(response));

        mockMvc.perform(post("/api/v1/evaluations/job-123/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void cancelJob_Success() throws Exception {
        doNothing().when(evaluationService).cancelJob("job-123");

        mockMvc.perform(post("/api/v1/evaluations/job-123/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listResults_Success() throws Exception {
        PageResponse<EvaluationResultResponse> pageResponse = PageResponse.of(
                Collections.singletonList(new EvaluationResultResponse()), 0, 20, 1);

        when(evaluationService.listResults(anyString(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/evaluations/job-123/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getResult_Success() throws Exception {
        EvaluationResultResponse response = new EvaluationResultResponse();
        response.setId("result-123");
        response.setJobId("job-123");
        response.setStatus("SUCCESS");

        when(evaluationService.getResult("result-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/evaluations/results/result-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("result-123"));
    }

    @Test
    void getMetrics_Success() throws Exception {
        EvaluationMetricsResponse metrics = new EvaluationMetricsResponse();
        metrics.setJobId("job-123");
        metrics.setSuccessRate(85.0);

        when(evaluationService.getMetrics("job-123")).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/evaluations/job-123/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.successRate").value(85.0));
    }

    @Test
    void compareJobs_Success() throws Exception {
        EvaluationCompareRequest request = new EvaluationCompareRequest();
        request.setJobId1("job-123");
        request.setJobId2("job-456");

        EvaluationCompareResponse compareResponse = new EvaluationCompareResponse();
        EvaluationJobResponse job1 = new EvaluationJobResponse();
        job1.setId("job-123");
        EvaluationJobResponse job2 = new EvaluationJobResponse();
        job2.setId("job-456");
        compareResponse.setJob1(job1);
        compareResponse.setJob2(job2);

        when(evaluationService.compareJobs(any())).thenReturn(compareResponse);

        mockMvc.perform(post("/api/v1/evaluations/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(200));
    }
}

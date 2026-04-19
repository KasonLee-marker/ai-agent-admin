package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.*;
import com.aiagent.admin.domain.entity.EvaluationJob;
import com.aiagent.admin.domain.entity.EvaluationResult;
import com.aiagent.admin.domain.repository.*;
import com.aiagent.admin.service.impl.EvaluationAsyncService;
import com.aiagent.admin.service.impl.EvaluationServiceImpl;
import com.aiagent.admin.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationJobRepository evaluationJobRepository;
    @Mock
    private EvaluationResultRepository evaluationResultRepository;
    @Mock
    private DatasetRepository datasetRepository;
    @Mock
    private DatasetItemRepository datasetItemRepository;
    @Mock
    private PromptTemplateRepository promptTemplateRepository;
    @Mock
    private ModelConfigRepository modelConfigRepository;
    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private ModelConfigService modelConfigService;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private EvaluationAsyncService evaluationAsyncService;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private EvaluationJob testJob;
    private EvaluationJobResponse testJobResponse;

    @BeforeEach
    void setUp() {
        testJob = EvaluationJob.builder()
                .id("job-123")
                .name("Test Job")
                .description("Test Description")
                .promptTemplateId("prompt-123")
                .modelConfigId("model-123")
                .datasetId("dataset-123")
                .status(EvaluationJob.JobStatus.PENDING)
                .totalItems(10)
                .completedItems(0)
                .successCount(0)
                .failedCount(0)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testJobResponse = new EvaluationJobResponse();
        testJobResponse.setId("job-123");
        testJobResponse.setName("Test Job");
        testJobResponse.setStatus("PENDING");
    }

    @Test
    void createJob_Success() {
        EvaluationJobCreateRequest request = new EvaluationJobCreateRequest();
        request.setName("Test Job");
        request.setDescription("Test Description");
        request.setPromptTemplateId("prompt-123");
        request.setModelConfigId("model-123");
        request.setDatasetId("dataset-123");
        request.setCreatedBy("test-user");

        when(evaluationMapper.toJobEntity(request)).thenReturn(testJob);
        when(evaluationJobRepository.save(any(EvaluationJob.class))).thenReturn(testJob);
        when(evaluationMapper.toJobResponse(testJob)).thenReturn(testJobResponse);

        EvaluationJobResponse result = evaluationService.createJob(request);

        assertNotNull(result);
        assertEquals("job-123", result.getId());
        verify(evaluationJobRepository).save(any(EvaluationJob.class));
    }

    @Test
    void getJob_Success() {
        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(evaluationMapper.toJobResponse(testJob)).thenReturn(testJobResponse);

        EvaluationJobResponse result = evaluationService.getJob("job-123");

        assertNotNull(result);
        assertEquals("job-123", result.getId());
    }

    @Test
    void getJob_NotFound_ThrowsException() {
        when(evaluationJobRepository.findById("non-existent")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> evaluationService.getJob("non-existent"));
    }

    @Test
    void updateJob_Success() {
        EvaluationJobUpdateRequest request = new EvaluationJobUpdateRequest();
        request.setName("Updated Job");

        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(evaluationJobRepository.save(any(EvaluationJob.class))).thenReturn(testJob);
        when(evaluationMapper.toJobResponse(testJob)).thenReturn(testJobResponse);

        EvaluationJobResponse result = evaluationService.updateJob("job-123", request);

        assertNotNull(result);
        verify(evaluationMapper).updateJobEntity(testJob, request);
    }

    @Test
    void updateJob_RunningJob_ThrowsException() {
        testJob.setStatus(EvaluationJob.JobStatus.RUNNING);
        EvaluationJobUpdateRequest request = new EvaluationJobUpdateRequest();
        request.setName("Updated Job");

        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        assertThrows(IllegalStateException.class, () -> evaluationService.updateJob("job-123", request));
    }

    @Test
    void deleteJob_Success() {
        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        evaluationService.deleteJob("job-123");

        verify(evaluationResultRepository).deleteByJobId("job-123");
        verify(evaluationJobRepository).delete(testJob);
    }

    @Test
    void deleteJob_RunningJob_ThrowsException() {
        testJob.setStatus(EvaluationJob.JobStatus.RUNNING);
        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        assertThrows(IllegalStateException.class, () -> evaluationService.deleteJob("job-123"));
    }

    @Test
    void listJobs_Success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EvaluationJob> jobPage = new PageImpl<>(Collections.singletonList(testJob), pageable, 1);

        when(evaluationJobRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(jobPage);
        when(evaluationMapper.toJobResponse(testJob)).thenReturn(testJobResponse);

        PageResponse<EvaluationJobResponse> result = evaluationService.listJobs(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void cancelJob_Success() {
        testJob.setStatus(EvaluationJob.JobStatus.RUNNING);
        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        evaluationService.cancelJob("job-123");

        verify(evaluationJobRepository).findById("job-123");
        verify(evaluationAsyncService).requestCancellation("job-123");
    }

    @Test
    void cancelJob_NotRunning_ThrowsException() {
        testJob.setStatus(EvaluationJob.JobStatus.PENDING);
        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        assertThrows(IllegalStateException.class, () -> evaluationService.cancelJob("job-123"));
    }

    @Test
    void getMetrics_Success() {
        testJob.setCompletedItems(10);
        testJob.setSuccessCount(8);
        testJob.setFailedCount(2);
        testJob.setTotalLatencyMs(5000L);

        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(evaluationMapper.toMetricsResponse(testJob)).thenReturn(new EvaluationMetricsResponse());
        // Mock 平均值查询
        when(evaluationResultRepository.calculateAverageScoreByJobId("job-123")).thenReturn(85.0);
        when(evaluationResultRepository.calculateAverageSemanticSimilarityByJobId("job-123")).thenReturn(0.8);
        when(evaluationResultRepository.calculateAverageFaithfulnessByJobId("job-123")).thenReturn(1.0);

        EvaluationMetricsResponse metrics = evaluationService.getMetrics("job-123");

        assertNotNull(metrics);
        verify(evaluationResultRepository).calculateAverageScoreByJobId("job-123");
    }

    @Test
    void compareJobs_Success() {
        EvaluationJob job2 = EvaluationJob.builder()
                .id("job-456")
                .name("Test Job 2")
                .status(EvaluationJob.JobStatus.COMPLETED)
                .build();
        EvaluationJobResponse job2Response = new EvaluationJobResponse();
        job2Response.setId("job-456");

        EvaluationCompareRequest request = new EvaluationCompareRequest();
        request.setJobId1("job-123");
        request.setJobId2("job-456");

        when(evaluationJobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(evaluationJobRepository.findById("job-456")).thenReturn(Optional.of(job2));
        when(evaluationMapper.toJobResponse(testJob)).thenReturn(testJobResponse);
        when(evaluationMapper.toJobResponse(job2)).thenReturn(job2Response);
        when(evaluationMapper.toComparisonMetrics(testJob, job2)).thenReturn(new EvaluationCompareResponse.ComparisonMetrics());

        EvaluationCompareResponse response = evaluationService.compareJobs(request);

        assertNotNull(response);
        assertNotNull(response.getJob1());
        assertNotNull(response.getJob2());
    }

    @Test
    void listResults_Success() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt"));
        EvaluationResult result = EvaluationResult.builder()
                .id("result-123")
                .jobId("job-123")
                .status(EvaluationResult.ResultStatus.SUCCESS)
                .build();
        Page<EvaluationResult> resultPage = new PageImpl<>(Collections.singletonList(result), pageable, 1);
        EvaluationResultResponse response = new EvaluationResultResponse();
        response.setId("result-123");

        when(evaluationResultRepository.findByJobIdOrderByCreatedAtAsc("job-123", pageable)).thenReturn(resultPage);
        when(evaluationMapper.toResultResponse(result)).thenReturn(response);

        PageResponse<EvaluationResultResponse> resultResponse = evaluationService.listResults("job-123", pageable);

        assertNotNull(resultResponse);
        assertEquals(1, resultResponse.getTotalElements());
    }

    @Test
    void getResult_Success() {
        EvaluationResult result = EvaluationResult.builder()
                .id("result-123")
                .jobId("job-123")
                .status(EvaluationResult.ResultStatus.SUCCESS)
                .build();
        EvaluationResultResponse response = new EvaluationResultResponse();
        response.setId("result-123");

        when(evaluationResultRepository.findById("result-123")).thenReturn(Optional.of(result));
        when(evaluationMapper.toResultResponse(result)).thenReturn(response);

        EvaluationResultResponse resultResponse = evaluationService.getResult("result-123");

        assertNotNull(resultResponse);
        assertEquals("result-123", resultResponse.getId());
    }
}

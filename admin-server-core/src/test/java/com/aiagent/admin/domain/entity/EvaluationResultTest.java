package com.aiagent.admin.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationResultTest {

    @Test
    void buildResult_Success() {
        EvaluationResult result = EvaluationResult.builder()
                .id("result-123")
                .jobId("job-123")
                .datasetItemId("item-123")
                .input("Test input")
                .expectedOutput("Expected output")
                .actualOutput("Actual output")
                .latencyMs(500)
                .inputTokens(100)
                .outputTokens(50)
                .status(EvaluationResult.ResultStatus.SUCCESS)
                .errorMessage(null)
                .build();

        assertNotNull(result);
        assertEquals("result-123", result.getId());
        assertEquals("job-123", result.getJobId());
        assertEquals("item-123", result.getDatasetItemId());
        assertEquals("Test input", result.getInput());
        assertEquals("Expected output", result.getExpectedOutput());
        assertEquals("Actual output", result.getActualOutput());
        assertEquals(500, result.getLatencyMs());
        assertEquals(100, result.getInputTokens());
        assertEquals(50, result.getOutputTokens());
        assertEquals(EvaluationResult.ResultStatus.SUCCESS, result.getStatus());
    }

    @Test
    void resultStatus_EnumValues() {
        assertEquals(3, EvaluationResult.ResultStatus.values().length);
        assertNotNull(EvaluationResult.ResultStatus.PENDING);
        assertNotNull(EvaluationResult.ResultStatus.SUCCESS);
        assertNotNull(EvaluationResult.ResultStatus.FAILED);
    }

    @Test
    void defaultStatus_IsPending() {
        EvaluationResult result = new EvaluationResult();
        assertEquals(EvaluationResult.ResultStatus.PENDING, result.getStatus());
    }
}

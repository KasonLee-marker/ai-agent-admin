package com.aiagent.admin.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationJobTest {

    @Test
    void incrementCompleted_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .completedItems(5)
                .build();

        job.incrementCompleted();

        assertEquals(6, job.getCompletedItems());
    }

    @Test
    void incrementSuccess_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .successCount(3)
                .build();

        job.incrementSuccess();

        assertEquals(4, job.getSuccessCount());
    }

    @Test
    void incrementFailed_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .failedCount(2)
                .build();

        job.incrementFailed();

        assertEquals(3, job.getFailedCount());
    }

    @Test
    void addLatency_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .totalLatencyMs(1000L)
                .build();

        job.addLatency(500);

        assertEquals(1500L, job.getTotalLatencyMs());
    }

    @Test
    void addLatency_NullInitial_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .totalLatencyMs(null)
                .build();

        job.addLatency(500);

        assertEquals(500L, job.getTotalLatencyMs());
    }

    @Test
    void addInputTokens_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .totalInputTokens(1000L)
                .build();

        job.addInputTokens(500);

        assertEquals(1500L, job.getTotalInputTokens());
    }

    @Test
    void addOutputTokens_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .totalOutputTokens(500L)
                .build();

        job.addOutputTokens(300);

        assertEquals(800L, job.getTotalOutputTokens());
    }

    @Test
    void getAverageLatencyMs_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .totalLatencyMs(5000L)
                .completedItems(10)
                .build();

        Double avg = job.getAverageLatencyMs();

        assertNotNull(avg);
        assertEquals(500.0, avg);
    }

    @Test
    void getAverageLatencyMs_NoCompletedItems_ReturnsNull() {
        EvaluationJob job = EvaluationJob.builder()
                .totalLatencyMs(5000L)
                .completedItems(0)
                .build();

        Double avg = job.getAverageLatencyMs();

        assertNull(avg);
    }

    @Test
    void getSuccessRate_Success() {
        EvaluationJob job = EvaluationJob.builder()
                .successCount(8)
                .completedItems(10)
                .build();

        Double rate = job.getSuccessRate();

        assertNotNull(rate);
        assertEquals(80.0, rate);
    }

    @Test
    void getSuccessRate_NoCompletedItems_ReturnsNull() {
        EvaluationJob job = EvaluationJob.builder()
                .successCount(0)
                .completedItems(0)
                .build();

        Double rate = job.getSuccessRate();

        assertNull(rate);
    }

    @Test
    void jobStatus_EnumValues() {
        assertEquals(5, EvaluationJob.JobStatus.values().length);
        assertNotNull(EvaluationJob.JobStatus.PENDING);
        assertNotNull(EvaluationJob.JobStatus.RUNNING);
        assertNotNull(EvaluationJob.JobStatus.COMPLETED);
        assertNotNull(EvaluationJob.JobStatus.FAILED);
        assertNotNull(EvaluationJob.JobStatus.CANCELLED);
    }
}

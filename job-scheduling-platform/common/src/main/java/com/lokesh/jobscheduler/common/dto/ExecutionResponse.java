package com.lokesh.jobscheduler.common.dto;

import com.lokesh.jobscheduler.common.enums.ExecutionStatus;
import com.lokesh.jobscheduler.common.enums.Priority;
import com.lokesh.jobscheduler.common.enums.TriggerType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        String executionId,
        UUID jobId,
        String jobName,
        String tenantId,
        ExecutionStatus status,
        TriggerType triggerType,
        Priority priority,
        int attempt,
        int maxAttempts,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Long durationMs,
        String output,
        String errorMessage,
        String workerId,
        LocalDateTime createdAt
) {
}

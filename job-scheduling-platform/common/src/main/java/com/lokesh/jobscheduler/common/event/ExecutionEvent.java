package com.lokesh.jobscheduler.common.event;

import com.lokesh.jobscheduler.common.enums.ExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record ExecutionEvent(
        String eventType,
        String executionId,
        UUID jobId,
        String tenantId,
        ExecutionStatus status,
        String message,
        Instant timestamp,
        String workerId
) {
}

package com.lokesh.jobscheduler.common.dto;

import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.common.enums.JobType;
import com.lokesh.jobscheduler.common.enums.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String tenantId,
        String name,
        String description,
        JobType jobType,
        Map<String, Object> configuration,
        String cronExpression,
        Priority priority,
        JobStatus status,
        int maxRetries,
        int timeoutSeconds,
        List<String> tags,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        LocalDateTime lastExecutedAt,
        LocalDateTime nextRunAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

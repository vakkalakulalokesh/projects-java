package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.gateway.common.enums.StepExecutionStatus;
import com.lokesh.gateway.common.enums.StepType;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StepExecutionResponse(
        String id,
        String stepId,
        String stepName,
        StepType stepType,
        StepExecutionStatus status,
        Map<String, Object> inputData,
        Map<String, Object> outputData,
        String error,
        int attempt,
        Instant startedAt,
        Instant completedAt,
        Long durationMs
) {
}

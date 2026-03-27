package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.gateway.common.enums.ExecutionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FlowExecutionResponse(
        String id,
        String executionId,
        String flowId,
        String flowName,
        ExecutionStatus status,
        Map<String, Object> inputData,
        Map<String, Object> outputData,
        String correlationId,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        List<StepExecutionResponse> stepExecutions,
        Instant createdAt
) {
}

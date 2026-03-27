package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.gateway.common.enums.ExecutionStatus;
import com.lokesh.gateway.common.enums.StepExecutionStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExecutionTraceEvent(
        String eventType,
        String executionId,
        String stepId,
        String stepName,
        Object status,
        String message,
        Map<String, Object> data,
        Instant timestamp
) {
    public static ExecutionTraceEvent stepStarted(String executionId, String stepId, String stepName) {
        return new ExecutionTraceEvent(
                "STEP_STARTED",
                executionId,
                stepId,
                stepName,
                StepExecutionStatus.RUNNING,
                null,
                null,
                Instant.now()
        );
    }

    public static ExecutionTraceEvent stepCompleted(String executionId, String stepId, String stepName,
                                                     Map<String, Object> data) {
        return new ExecutionTraceEvent(
                "STEP_COMPLETED",
                executionId,
                stepId,
                stepName,
                StepExecutionStatus.COMPLETED,
                null,
                data,
                Instant.now()
        );
    }

    public static ExecutionTraceEvent stepFailed(String executionId, String stepId, String stepName, String message) {
        return new ExecutionTraceEvent(
                "STEP_FAILED",
                executionId,
                stepId,
                stepName,
                StepExecutionStatus.FAILED,
                message,
                null,
                Instant.now()
        );
    }

    public static ExecutionTraceEvent flowCompleted(String executionId, Map<String, Object> data) {
        return new ExecutionTraceEvent(
                "FLOW_COMPLETED",
                executionId,
                null,
                null,
                ExecutionStatus.COMPLETED,
                null,
                data,
                Instant.now()
        );
    }

    public static ExecutionTraceEvent flowFailed(String executionId, String message) {
        return new ExecutionTraceEvent(
                "FLOW_FAILED",
                executionId,
                null,
                null,
                ExecutionStatus.FAILED,
                message,
                null,
                Instant.now()
        );
    }
}

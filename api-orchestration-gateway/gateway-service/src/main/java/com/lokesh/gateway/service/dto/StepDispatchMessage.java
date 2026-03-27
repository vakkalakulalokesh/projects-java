package com.lokesh.gateway.service.dto;

import com.lokesh.gateway.common.dto.RetryConfig;
import com.lokesh.gateway.common.enums.StepType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDispatchMessage {

    private UUID flowExecutionPk;
    private String executionId;
    private String stepId;
    private String stepName;
    private StepType stepType;
    private Map<String, Object> config;
    private Map<String, Object> inputData;
    private RetryConfig retryConfig;
    private Map<String, Object> compensationConfig;
    private int timeoutSeconds;
    private Integer priority;
}

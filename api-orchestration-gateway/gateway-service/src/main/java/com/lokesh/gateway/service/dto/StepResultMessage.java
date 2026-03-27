package com.lokesh.gateway.service.dto;

import com.lokesh.gateway.common.enums.StepExecutionStatus;
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
public class StepResultMessage {

    private UUID flowExecutionPk;
    private String executionId;
    private String stepId;
    private String stepName;
    private StepType stepType;
    private StepExecutionStatus status;
    private Map<String, Object> outputData;
    private String errorMessage;
    private Long durationMs;
    private String branchTargetStepId;
    private Boolean compensation;
}

package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.gateway.common.enums.StepType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepDefinition {

    private String stepId;
    private String name;
    private StepType type;
    private Map<String, Object> config;

    private RetryConfig retryConfig;

    @Builder.Default
    private int timeoutSeconds = 30;

    private Map<String, Object> compensationConfig;
}

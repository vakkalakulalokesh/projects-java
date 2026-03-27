package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.gateway.common.enums.CircuitState;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CircuitBreakerStatus(
        String endpoint,
        CircuitState state,
        int failureCount,
        int successCount,
        Instant lastFailureAt,
        Instant lastSuccessAt,
        Instant nextRetryAt
) {
}

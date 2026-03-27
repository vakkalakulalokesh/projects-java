package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardStats(
        long totalFlows,
        long activeFlows,
        long totalExecutions,
        long runningExecutions,
        long completedToday,
        long failedToday,
        double successRate,
        double avgDurationMs,
        List<CircuitBreakerStatus> circuitBreakers
) {
}

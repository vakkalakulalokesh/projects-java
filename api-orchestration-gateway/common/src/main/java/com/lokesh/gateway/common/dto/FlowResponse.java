package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.gateway.common.enums.FlowStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FlowResponse(
        String id,
        String name,
        String description,
        int version,
        FlowDefinition flowDefinition,
        Map<String, Object> inputSchema,
        FlowStatus status,
        List<String> tags,
        long totalExecutions,
        Double avgDurationMs,
        Double successRate,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}

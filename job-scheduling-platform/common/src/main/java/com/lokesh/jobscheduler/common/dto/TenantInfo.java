package com.lokesh.jobscheduler.common.dto;

public record TenantInfo(
        String tenantId,
        String name,
        long jobCount,
        int executionQuota,
        int executionsUsed
) {
}

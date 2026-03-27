package com.lokesh.jobscheduler.common.dto;

import com.lokesh.jobscheduler.common.enums.ExecutionStatus;

import java.util.List;
import java.util.Map;

public record DashboardStats(
        long totalJobs,
        long activeJobs,
        long totalExecutionsToday,
        long runningExecutions,
        long completedToday,
        long failedToday,
        double successRate,
        long workerCount,
        Double avgDurationMs,
        Map<ExecutionStatus, Long> executionsByStatus,
        List<HourlyCount> executionsByHour
) {
}

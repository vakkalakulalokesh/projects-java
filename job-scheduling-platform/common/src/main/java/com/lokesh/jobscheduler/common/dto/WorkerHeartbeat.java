package com.lokesh.jobscheduler.common.dto;

import java.time.LocalDateTime;

public record WorkerHeartbeat(
        String workerId,
        String hostname,
        int activeExecutions,
        int maxConcurrency,
        String status,
        LocalDateTime lastHeartbeatAt,
        long uptime
) {
}

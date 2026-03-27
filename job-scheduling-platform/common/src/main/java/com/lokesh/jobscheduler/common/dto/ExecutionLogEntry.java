package com.lokesh.jobscheduler.common.dto;

import java.time.LocalDateTime;

public record ExecutionLogEntry(
        LocalDateTime timestamp,
        String level,
        String message,
        String workerId
) {
}

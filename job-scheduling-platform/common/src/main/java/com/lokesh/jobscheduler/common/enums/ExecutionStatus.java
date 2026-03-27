package com.lokesh.jobscheduler.common.enums;

public enum ExecutionStatus {
    PENDING,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    RETRYING,
    DEAD_LETTERED
}

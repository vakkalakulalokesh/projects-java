package com.lokesh.jobscheduler.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Priority {
    LOW(1),
    MEDIUM(5),
    HIGH(8),
    CRITICAL(10);

    private final int value;
}

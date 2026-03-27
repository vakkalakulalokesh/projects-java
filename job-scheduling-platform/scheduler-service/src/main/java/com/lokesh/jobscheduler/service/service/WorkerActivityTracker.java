package com.lokesh.jobscheduler.service.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WorkerActivityTracker {

    private final AtomicInteger activeExecutions = new AtomicInteger(0);

    public int beginExecution() {
        return activeExecutions.incrementAndGet();
    }

    public int endExecution() {
        return activeExecutions.decrementAndGet();
    }

    public int getActiveExecutions() {
        return activeExecutions.get();
    }
}

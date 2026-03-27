package com.lokesh.jobscheduler.service.rabbitmq;

import com.lokesh.jobscheduler.common.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionMessage {

    private String executionId;
    private UUID jobId;
    private String tenantId;
    private String jobName;
    private JobType jobType;
    private Map<String, Object> configuration;
    private int attempt;
    private int maxAttempts;
    private int timeoutSeconds;
    private int priority;
}

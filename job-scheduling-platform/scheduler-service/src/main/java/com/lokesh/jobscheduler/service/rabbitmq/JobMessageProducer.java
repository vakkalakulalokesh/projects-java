package com.lokesh.jobscheduler.service.rabbitmq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.jobscheduler.common.event.ExecutionEvent;
import com.lokesh.jobscheduler.service.entity.ExecutionEntity;
import com.lokesh.jobscheduler.service.entity.JobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void dispatchJob(ExecutionEntity execution) {
        JobEntity job = execution.getJob();
        JobExecutionMessage message = JobExecutionMessage.builder()
                .executionId(execution.getExecutionId())
                .jobId(job.getId())
                .tenantId(execution.getTenantId())
                .jobName(execution.getJobName())
                .jobType(job.getJobType())
                .configuration(readConfig(job.getConfiguration()))
                .attempt(execution.getAttempt())
                .maxAttempts(execution.getMaxAttempts())
                .timeoutSeconds(job.getTimeoutSeconds())
                .priority(execution.getPriority())
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMqNames.EXCHANGE_JOB_EXECUTION,
                RabbitMqNames.RK_JOB_DISPATCH,
                message,
                m -> {
                    Integer prio = message.getPriority();
                    if (prio != null) {
                        m.getMessageProperties().setPriority(prio);
                    }
                    return m;
                });
    }

    public void sendToRetryQueue(JobExecutionMessage message, long delayMs) {
        rabbitTemplate.convertAndSend(
                RabbitMqNames.EXCHANGE_JOB_RETRY,
                RabbitMqNames.RK_JOB_RETRY,
                message,
                m -> {
                    m.getMessageProperties().setExpiration(String.valueOf(delayMs));
                    Integer prio = message.getPriority();
                    if (prio != null) {
                        m.getMessageProperties().setPriority(prio);
                    }
                    return m;
                });
    }

    public void sendToDlq(JobExecutionMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqNames.EXCHANGE_JOB_DLX,
                RabbitMqNames.RK_JOB_DLQ,
                message);
    }

    public void broadcastEvent(ExecutionEvent event) {
        rabbitTemplate.convertAndSend(RabbitMqNames.EXCHANGE_JOB_EVENTS, "", event);
    }

    private Map<String, Object> readConfig(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }
}

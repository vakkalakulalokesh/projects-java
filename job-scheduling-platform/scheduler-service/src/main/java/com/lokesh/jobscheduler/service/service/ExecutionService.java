package com.lokesh.jobscheduler.service.service;

import com.lokesh.jobscheduler.common.dto.DashboardStats;
import com.lokesh.jobscheduler.common.dto.ExecutionResponse;
import com.lokesh.jobscheduler.common.dto.HourlyCount;
import com.lokesh.jobscheduler.common.enums.ExecutionStatus;
import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.common.enums.Priority;
import com.lokesh.jobscheduler.common.enums.TriggerType;
import com.lokesh.jobscheduler.common.event.ExecutionEvent;
import com.lokesh.jobscheduler.common.exception.JobSchedulerException;
import com.lokesh.jobscheduler.service.entity.ExecutionEntity;
import com.lokesh.jobscheduler.service.entity.JobEntity;
import com.lokesh.jobscheduler.service.repository.ExecutionRepository;
import com.lokesh.jobscheduler.service.repository.JobRepository;
import com.lokesh.jobscheduler.service.rabbitmq.JobMessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final JobRepository jobRepository;
    private final JobMessageProducer jobMessageProducer;

    @Transactional
    public ExecutionResponse createExecution(JobEntity job, TriggerType triggerType) {
        String executionId = "exec-" + UUID.randomUUID().toString().replace("-", "");
        ExecutionEntity entity = ExecutionEntity.builder()
                .executionId(executionId)
                .job(job)
                .jobName(job.getName())
                .tenantId(job.getTenantId())
                .status(ExecutionStatus.PENDING)
                .triggerType(triggerType)
                .priority(job.getPriority())
                .attempt(1)
                .maxAttempts(job.getMaxRetries() + 1)
                .build();
        executionRepository.save(entity);
        executionRepository.flush();
        jobMessageProducer.dispatchJob(entity);
        entity.setStatus(ExecutionStatus.QUEUED);
        executionRepository.save(entity);
        publishEvent("STARTED", entity, "Execution queued", null);
        return toResponse(entity);
    }

    @Transactional
    public ExecutionEntity updateExecutionStatus(
            String executionId,
            ExecutionStatus status,
            String output,
            String errorMessage,
            String workerId,
            Long durationMs) {
        ExecutionEntity entity = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new JobSchedulerException("EXECUTION_NOT_FOUND", "Execution not found: " + executionId));
        entity.setStatus(status);
        if (output != null) {
            entity.setOutput(output);
        }
        if (errorMessage != null) {
            entity.setErrorMessage(errorMessage);
        }
        if (workerId != null) {
            entity.setWorkerId(workerId);
        }
        if (durationMs != null) {
            entity.setDurationMs(durationMs);
        }
        if (status == ExecutionStatus.RUNNING && entity.getStartedAt() == null) {
            entity.setStartedAt(LocalDateTime.now());
        }
        if (status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED
                || status == ExecutionStatus.TIMED_OUT || status == ExecutionStatus.DEAD_LETTERED) {
            entity.setCompletedAt(LocalDateTime.now());
        }
        executionRepository.save(entity);
        String eventType = mapEventType(status);
        publishEvent(eventType, entity, status.name(), workerId);
        return entity;
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(String executionId) {
        ExecutionEntity entity = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new JobSchedulerException("EXECUTION_NOT_FOUND", "Execution not found: " + executionId));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<ExecutionResponse> listExecutions(
            String tenantId,
            UUID jobId,
            ExecutionStatus status,
            Pageable pageable) {
        return executionRepository.findByTenantIdAndOptionalJobAndStatus(tenantId, jobId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats(String tenantId) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long totalExecutionsToday = executionRepository.countByTenantIdAndCreatedAtAfter(tenantId, startOfDay);
        long completedToday = executionRepository.countByTenantIdAndStatusAndCreatedAtAfter(
                tenantId, ExecutionStatus.COMPLETED, startOfDay);
        long failedToday = executionRepository.countByTenantIdAndStatusAndCreatedAtAfter(
                tenantId, ExecutionStatus.FAILED, startOfDay)
                + executionRepository.countByTenantIdAndStatusAndCreatedAtAfter(
                tenantId, ExecutionStatus.DEAD_LETTERED, startOfDay)
                + executionRepository.countByTenantIdAndStatusAndCreatedAtAfter(
                tenantId, ExecutionStatus.TIMED_OUT, startOfDay);
        long runningExecutions = executionRepository.countActiveByTenantId(tenantId);
        long totalJobs = jobRepository.countByTenantId(tenantId);
        long activeJobs = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.ACTIVE);

        double successRate = totalExecutionsToday == 0 ? 0.0
                : (completedToday * 100.0) / totalExecutionsToday;

        Map<ExecutionStatus, Long> byStatus = new EnumMap<>(ExecutionStatus.class);
        List<Object[]> rows = executionRepository.executionsByStatusToday(tenantId, startOfDay);
        for (Object[] row : rows) {
            byStatus.put((ExecutionStatus) row[0], (Long) row[1]);
        }

        List<HourlyCount> hourly = executionRepository.hourlyExecutionCounts(tenantId, startOfDay).stream()
                .map(r -> new HourlyCount(((Number) r[0]).intValue(), ((Number) r[1]).longValue()))
                .toList();

        Double avgDuration = executionRepository.avgDurationMsSince(tenantId, startOfDay);

        return new DashboardStats(
                totalJobs,
                activeJobs,
                totalExecutionsToday,
                runningExecutions,
                completedToday,
                failedToday,
                successRate,
                0L,
                avgDuration,
                byStatus,
                hourly
        );
    }

    public DashboardStats withWorkerCount(DashboardStats base, long workerCount) {
        return new DashboardStats(
                base.totalJobs(),
                base.activeJobs(),
                base.totalExecutionsToday(),
                base.runningExecutions(),
                base.completedToday(),
                base.failedToday(),
                base.successRate(),
                workerCount,
                base.avgDurationMs(),
                base.executionsByStatus(),
                base.executionsByHour()
        );
    }

    @Transactional
    public void incrementJobSuccess(JobEntity job) {
        JobEntity managed = jobRepository.findById(job.getId()).orElseThrow();
        managed.setSuccessfulExecutions(managed.getSuccessfulExecutions() + 1);
        managed.setTotalExecutions(managed.getTotalExecutions() + 1);
        managed.setLastExecutedAt(LocalDateTime.now());
        jobRepository.save(managed);
    }

    @Transactional
    public void incrementJobFailure(JobEntity job) {
        JobEntity managed = jobRepository.findById(job.getId()).orElseThrow();
        managed.setFailedExecutions(managed.getFailedExecutions() + 1);
        managed.setTotalExecutions(managed.getTotalExecutions() + 1);
        managed.setLastExecutedAt(LocalDateTime.now());
        jobRepository.save(managed);
    }

    private void publishEvent(String eventType, ExecutionEntity entity, String message, String workerId) {
        ExecutionEvent event = new ExecutionEvent(
                eventType,
                entity.getExecutionId(),
                entity.getJob().getId(),
                entity.getTenantId(),
                entity.getStatus(),
                message,
                Instant.now(),
                workerId
        );
        jobMessageProducer.broadcastEvent(event);
    }

    private static String mapEventType(ExecutionStatus status) {
        return switch (status) {
            case RUNNING -> "PROGRESS";
            case COMPLETED -> "COMPLETED";
            case FAILED, DEAD_LETTERED, TIMED_OUT -> "FAILED";
            default -> "STARTED";
        };
    }

    private ExecutionResponse toResponse(ExecutionEntity e) {
        return new ExecutionResponse(
                e.getId(),
                e.getExecutionId(),
                e.getJob().getId(),
                e.getJobName(),
                e.getTenantId(),
                e.getStatus(),
                e.getTriggerType(),
                priorityFromInt(e.getPriority()),
                e.getAttempt(),
                e.getMaxAttempts(),
                e.getStartedAt(),
                e.getCompletedAt(),
                e.getDurationMs(),
                e.getOutput(),
                e.getErrorMessage(),
                e.getWorkerId(),
                e.getCreatedAt()
        );
    }

    private Priority priorityFromInt(int p) {
        for (Priority pr : Priority.values()) {
            if (pr.getValue() == p) {
                return pr;
            }
        }
        return Priority.MEDIUM;
    }
}

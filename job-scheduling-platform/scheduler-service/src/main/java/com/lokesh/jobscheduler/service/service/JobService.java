package com.lokesh.jobscheduler.service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.jobscheduler.common.dto.ExecutionResponse;
import com.lokesh.jobscheduler.common.dto.JobCreateRequest;
import com.lokesh.jobscheduler.common.dto.JobResponse;
import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.common.enums.Priority;
import com.lokesh.jobscheduler.common.enums.TriggerType;
import com.lokesh.jobscheduler.common.exception.JobSchedulerException;
import com.lokesh.jobscheduler.service.entity.JobEntity;
import com.lokesh.jobscheduler.service.repository.JobRepository;
import com.lokesh.jobscheduler.service.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final TenantRepository tenantRepository;
    private final ExecutionService executionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        requireTenant(request.getTenantId());
        JobEntity entity = JobEntity.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .description(request.getDescription())
                .jobType(request.getJobType())
                .configuration(writeJson(request.getConfiguration()))
                .cronExpression(blankToNull(request.getCronExpression()))
                .priority(request.getPriority() != null ? request.getPriority().getValue() : Priority.MEDIUM.getValue())
                .status(JobStatus.ACTIVE)
                .timeoutSeconds(request.getTimeoutSeconds())
                .maxRetries(request.getMaxRetries())
                .tags(writeJson(request.getTags()))
                .nextRunAt(computeNextRun(blankToNull(request.getCronExpression())))
                .build();
        jobRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(String tenantId, UUID jobId) {
        JobEntity entity = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new JobSchedulerException("JOB_NOT_FOUND", "Job not found"));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> listJobs(String tenantId, JobStatus status, Pageable pageable) {
        Page<JobEntity> page = status == null
                ? jobRepository.findByTenantId(tenantId, pageable)
                : jobRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public JobResponse updateJob(String tenantId, UUID jobId, JobCreateRequest request) {
        JobEntity entity = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new JobSchedulerException("JOB_NOT_FOUND", "Job not found"));
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setJobType(request.getJobType());
        entity.setConfiguration(writeJson(request.getConfiguration()));
        entity.setCronExpression(blankToNull(request.getCronExpression()));
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority().getValue());
        }
        entity.setTimeoutSeconds(request.getTimeoutSeconds());
        entity.setMaxRetries(request.getMaxRetries());
        entity.setTags(writeJson(request.getTags()));
        entity.setNextRunAt(computeNextRun(entity.getCronExpression()));
        jobRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public void deleteJob(String tenantId, UUID jobId) {
        JobEntity entity = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new JobSchedulerException("JOB_NOT_FOUND", "Job not found"));
        entity.setStatus(JobStatus.DISABLED);
        jobRepository.save(entity);
    }

    @Transactional
    public JobResponse pauseJob(String tenantId, UUID jobId) {
        JobEntity entity = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new JobSchedulerException("JOB_NOT_FOUND", "Job not found"));
        entity.setStatus(JobStatus.PAUSED);
        jobRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public JobResponse resumeJob(String tenantId, UUID jobId) {
        JobEntity entity = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new JobSchedulerException("JOB_NOT_FOUND", "Job not found"));
        entity.setStatus(JobStatus.ACTIVE);
        entity.setNextRunAt(computeNextRun(entity.getCronExpression()));
        jobRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public ExecutionResponse triggerJob(String tenantId, UUID jobId) {
        JobEntity entity = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new JobSchedulerException("JOB_NOT_FOUND", "Job not found"));
        if (entity.getStatus() != JobStatus.ACTIVE) {
            throw new JobSchedulerException("JOB_INACTIVE", "Job is not active");
        }
        return executionService.createExecution(entity, TriggerType.MANUAL);
    }

    private void requireTenant(String tenantId) {
        tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new JobSchedulerException("TENANT_NOT_FOUND", "Tenant not found: " + tenantId));
    }

    private LocalDateTime computeNextRun(String cronExpression) {
        if (cronExpression == null) {
            return null;
        }
        try {
            CronExpression expr = CronExpression.parse(cronExpression.trim());
            return expr.next(LocalDateTime.now());
        } catch (Exception e) {
            throw new JobSchedulerException("INVALID_CRON", "Invalid cron expression", e);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new JobSchedulerException("SERIALIZATION_ERROR", "Failed to serialize JSON", e);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private JobResponse toResponse(JobEntity j) {
        return new JobResponse(
                j.getId(),
                j.getTenantId(),
                j.getName(),
                j.getDescription(),
                j.getJobType(),
                readMap(j.getConfiguration()),
                j.getCronExpression(),
                priorityFromInt(j.getPriority()),
                j.getStatus(),
                j.getMaxRetries(),
                j.getTimeoutSeconds(),
                readTags(j.getTags()),
                j.getTotalExecutions(),
                j.getSuccessfulExecutions(),
                j.getFailedExecutions(),
                j.getLastExecutedAt(),
                j.getNextRunAt(),
                j.getCreatedAt(),
                j.getUpdatedAt()
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

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}

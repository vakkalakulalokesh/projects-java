package com.lokesh.jobscheduler.service.service;

import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.common.enums.TriggerType;
import com.lokesh.jobscheduler.service.entity.JobEntity;
import com.lokesh.jobscheduler.service.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CronSchedulerService {

    private static final String LOCK_PREFIX = "cron:lock:";

    private final JobRepository jobRepository;
    private final ExecutionService executionService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedRate = 15_000)
    public void scanDueJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<JobEntity> candidates = jobRepository.findByStatusAndCronExpressionIsNotNull(JobStatus.ACTIVE);
        for (JobEntity job : candidates) {
            LocalDateTime next = job.getNextRunAt();
            if (next == null || next.isAfter(now)) {
                continue;
            }
            String lockKey = LOCK_PREFIX + job.getId() + ":" + next;
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(90));
            if (!Boolean.TRUE.equals(acquired)) {
                continue;
            }
            UUID jobId = job.getId();
            try {
                transactionTemplate.executeWithoutResult(status -> triggerScheduled(jobId));
            } catch (Exception ex) {
                log.warn("Scheduled trigger failed for job {}", jobId, ex);
            }
        }
    }

    private void triggerScheduled(UUID jobId) {
        JobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != JobStatus.ACTIVE || job.getCronExpression() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = job.getNextRunAt();
        if (nextRun == null || nextRun.isAfter(now)) {
            return;
        }
        executionService.createExecution(job, TriggerType.SCHEDULED);
        CronExpression expr = CronExpression.parse(job.getCronExpression().trim());
        LocalDateTime next = expr.next(LocalDateTime.now());
        job.setNextRunAt(next);
        jobRepository.save(job);
    }
}

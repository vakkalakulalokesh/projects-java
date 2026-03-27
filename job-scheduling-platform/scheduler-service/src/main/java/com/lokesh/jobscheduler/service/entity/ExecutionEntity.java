package com.lokesh.jobscheduler.service.entity;

import com.lokesh.jobscheduler.common.enums.ExecutionStatus;
import com.lokesh.jobscheduler.common.enums.TriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "executions",
        indexes = {
                @Index(name = "idx_exec_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_exec_job_created", columnList = "job_id, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String executionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Column(nullable = false, length = 512)
    private String jobName;

    @Column(nullable = false, length = 128)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExecutionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TriggerType triggerType;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    @Builder.Default
    private int attempt = 1;

    @Column(nullable = false)
    private int maxAttempts;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 256)
    private String workerId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getJobId() {
        return job != null ? job.getId() : null;
    }
}

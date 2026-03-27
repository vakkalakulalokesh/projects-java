package com.lokesh.jobscheduler.service.entity;

import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.common.enums.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "jobs",
        indexes = {
                @Index(name = "idx_jobs_tenant_status", columnList = "tenant_id, status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 128)
    private String tenantId;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobType jobType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String configuration;

    @Column(length = 128)
    private String cronExpression;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private int timeoutSeconds = 300;

    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(nullable = false)
    @Builder.Default
    private long totalExecutions = 0;

    @Column(nullable = false)
    @Builder.Default
    private long successfulExecutions = 0;

    @Column(nullable = false)
    @Builder.Default
    private long failedExecutions = 0;

    private LocalDateTime lastExecutedAt;

    private LocalDateTime nextRunAt;

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
}

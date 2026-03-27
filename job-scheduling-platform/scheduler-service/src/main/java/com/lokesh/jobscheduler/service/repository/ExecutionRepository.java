package com.lokesh.jobscheduler.service.repository;

import com.lokesh.jobscheduler.common.enums.ExecutionStatus;
import com.lokesh.jobscheduler.service.entity.ExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionRepository extends JpaRepository<ExecutionEntity, UUID> {

    Optional<ExecutionEntity> findByExecutionId(String executionId);

    @Query("SELECT e FROM ExecutionEntity e WHERE e.job.id = :jobId")
    Page<ExecutionEntity> findByJob_Id(@Param("jobId") UUID jobId, Pageable pageable);

    Page<ExecutionEntity> findByTenantId(String tenantId, Pageable pageable);

    Page<ExecutionEntity> findByTenantIdAndStatus(String tenantId, ExecutionStatus status, Pageable pageable);

    @Query("SELECT e FROM ExecutionEntity e WHERE e.tenantId = :tenantId AND (:jobId IS NULL OR e.job.id = :jobId) AND (:status IS NULL OR e.status = :status)")
    Page<ExecutionEntity> findByTenantIdAndOptionalJobAndStatus(
            @Param("tenantId") String tenantId,
            @Param("jobId") UUID jobId,
            @Param("status") ExecutionStatus status,
            Pageable pageable);

    List<ExecutionEntity> findByTenantIdAndStatus(String tenantId, ExecutionStatus status);

    List<ExecutionEntity> findByStatus(ExecutionStatus status);

    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.tenantId = :tenantId AND e.status = :status AND e.createdAt >= :after")
    long countByTenantIdAndStatusAndCreatedAtAfter(
            @Param("tenantId") String tenantId,
            @Param("status") ExecutionStatus status,
            @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.tenantId = :tenantId AND e.createdAt >= :after")
    long countByTenantIdAndCreatedAtAfter(@Param("tenantId") String tenantId, @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.tenantId = :tenantId AND e.status IN ('RUNNING','QUEUED')")
    long countActiveByTenantId(@Param("tenantId") String tenantId);

    @Query(value = """
            SELECT EXTRACT(HOUR FROM e.created_at) AS hr, COUNT(*) AS cnt
            FROM executions e
            WHERE e.tenant_id = :tenantId AND e.created_at >= :startOfDay
            GROUP BY EXTRACT(HOUR FROM e.created_at)
            ORDER BY hr
            """, nativeQuery = true)
    List<Object[]> hourlyExecutionCounts(@Param("tenantId") String tenantId, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("""
            SELECT e.status, COUNT(e)
            FROM ExecutionEntity e
            WHERE e.tenantId = :tenantId AND e.createdAt >= :startOfDay
            GROUP BY e.status
            """)
    List<Object[]> executionsByStatusToday(@Param("tenantId") String tenantId, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT AVG(e.durationMs) FROM ExecutionEntity e WHERE e.tenantId = :tenantId AND e.createdAt >= :after AND e.durationMs IS NOT NULL")
    Double avgDurationMsSince(@Param("tenantId") String tenantId, @Param("after") LocalDateTime after);
}

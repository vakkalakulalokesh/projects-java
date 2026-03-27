package com.lokesh.jobscheduler.service.repository;

import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.service.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    List<JobEntity> findByTenantId(String tenantId);

    Page<JobEntity> findByTenantId(String tenantId, Pageable pageable);

    Page<JobEntity> findByTenantIdAndStatus(String tenantId, JobStatus status, Pageable pageable);

    List<JobEntity> findByStatusAndCronExpressionIsNotNull(JobStatus status);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, JobStatus status);

    @Query("SELECT j FROM JobEntity j WHERE j.tenantId = :tenantId AND j.tags LIKE CONCAT('%', :tag, '%')")
    Page<JobEntity> findByTenantIdAndTagsContaining(@Param("tenantId") String tenantId,
                                                    @Param("tag") String tag,
                                                    Pageable pageable);

    Optional<JobEntity> findByIdAndTenantId(UUID id, String tenantId);
}

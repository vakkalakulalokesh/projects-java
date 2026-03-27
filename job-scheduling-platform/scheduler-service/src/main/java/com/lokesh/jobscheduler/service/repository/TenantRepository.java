package com.lokesh.jobscheduler.service.repository;

import com.lokesh.jobscheduler.service.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByTenantId(String tenantId);
}

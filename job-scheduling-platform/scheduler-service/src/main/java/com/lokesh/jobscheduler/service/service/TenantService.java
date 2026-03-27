package com.lokesh.jobscheduler.service.service;

import com.lokesh.jobscheduler.common.dto.TenantInfo;
import com.lokesh.jobscheduler.common.exception.JobSchedulerException;
import com.lokesh.jobscheduler.service.dto.TenantCreateRequest;
import com.lokesh.jobscheduler.service.entity.TenantEntity;
import com.lokesh.jobscheduler.service.repository.JobRepository;
import com.lokesh.jobscheduler.service.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final JobRepository jobRepository;

    @Transactional
    public TenantInfo createTenant(TenantCreateRequest request) {
        tenantRepository.findByTenantId(request.getTenantId()).ifPresent(t -> {
            throw new JobSchedulerException("TENANT_EXISTS", "Tenant already exists: " + request.getTenantId());
        });
        TenantEntity entity = TenantEntity.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .executionQuota(request.getExecutionQuota())
                .build();
        tenantRepository.save(entity);
        return toInfo(entity);
    }

    @Transactional(readOnly = true)
    public List<TenantInfo> listTenants() {
        return tenantRepository.findAll().stream().map(this::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public TenantInfo getTenant(String tenantId) {
        TenantEntity entity = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new JobSchedulerException("TENANT_NOT_FOUND", "Tenant not found: " + tenantId));
        return toInfo(entity);
    }

    private TenantInfo toInfo(TenantEntity entity) {
        long jobCount = jobRepository.countByTenantId(entity.getTenantId());
        return new TenantInfo(
                entity.getTenantId(),
                entity.getName(),
                jobCount,
                entity.getExecutionQuota(),
                entity.getExecutionsUsed()
        );
    }
}

package com.lokesh.gateway.service.repository;

import com.lokesh.gateway.common.enums.ExecutionStatus;
import com.lokesh.gateway.service.entity.FlowExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowExecutionRepository extends JpaRepository<FlowExecutionEntity, UUID> {

    Optional<FlowExecutionEntity> findByExecutionId(String executionId);

    Page<FlowExecutionEntity> findByFlow_Id(UUID flowId, Pageable pageable);

    Page<FlowExecutionEntity> findByFlow_IdAndStatus(UUID flowId, ExecutionStatus status, Pageable pageable);

    Page<FlowExecutionEntity> findByStatus(ExecutionStatus status, Pageable pageable);

    List<FlowExecutionEntity> findAllByStatus(ExecutionStatus status);

    List<FlowExecutionEntity> findByCorrelationId(String correlationId);

    long countByStatusAndCreatedAtAfter(ExecutionStatus status, Instant after);

    long countByStatus(ExecutionStatus status);
}

package com.lokesh.gateway.service.repository;

import com.lokesh.gateway.service.entity.FlowExecutionEntity;
import com.lokesh.gateway.service.entity.StepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StepExecutionRepository extends JpaRepository<StepExecutionEntity, UUID> {

    List<StepExecutionEntity> findByFlowExecutionOrderByCreatedAtAsc(FlowExecutionEntity flowExecution);

    List<StepExecutionEntity> findByFlowExecution_IdOrderByCreatedAtAsc(UUID executionPk);

    Optional<StepExecutionEntity> findByFlowExecutionAndStepId(FlowExecutionEntity flowExecution, String stepId);

    Optional<StepExecutionEntity> findByFlowExecution_IdAndStepId(UUID flowExecutionPk, String stepId);
}

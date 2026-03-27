package com.lokesh.gateway.service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.gateway.common.dto.CircuitBreakerStatus;
import com.lokesh.gateway.common.dto.DashboardStats;
import com.lokesh.gateway.common.dto.ExecutionTraceEvent;
import com.lokesh.gateway.common.dto.FlowExecutionResponse;
import com.lokesh.gateway.common.dto.StepExecutionResponse;
import com.lokesh.gateway.common.enums.ExecutionStatus;
import com.lokesh.gateway.common.exception.GatewayException;
import com.lokesh.gateway.service.engine.CircuitBreakerManager;
import com.lokesh.gateway.service.entity.FlowExecutionEntity;
import com.lokesh.gateway.service.entity.StepExecutionEntity;
import com.lokesh.gateway.service.repository.FlowExecutionRepository;
import com.lokesh.gateway.service.repository.FlowRepository;
import com.lokesh.gateway.service.repository.StepExecutionRepository;
import com.lokesh.gateway.service.websocket.ExecutionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final FlowExecutionRepository flowExecutionRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final FlowRepository flowRepository;
    private final CircuitBreakerManager circuitBreakerManager;
    private final ExecutionWebSocketHandler executionWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public FlowExecutionResponse getExecution(String executionId) {
        FlowExecutionEntity ex = flowExecutionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new GatewayException("NOT_FOUND", "Execution not found"));
        List<StepExecutionEntity> steps = stepExecutionRepository.findByFlowExecution_IdOrderByCreatedAtAsc(ex.getId());
        return toResponse(ex, true);
    }

    @Transactional(readOnly = true)
    public Page<FlowExecutionResponse> listExecutions(UUID flowId, ExecutionStatus status, int page, int size) {
        Pageable p = PageRequest.of(page, size);
        Page<FlowExecutionEntity> result;
        if (flowId != null && status != null) {
            result = flowExecutionRepository.findByFlow_IdAndStatus(flowId, status, p);
        } else if (flowId != null) {
            result = flowExecutionRepository.findByFlow_Id(flowId, p);
        } else if (status != null) {
            result = flowExecutionRepository.findByStatus(status, p);
        } else {
            result = flowExecutionRepository.findAll(p);
        }
        return result.map(e -> toResponse(e, false));
    }

    @Transactional
    public void cancelExecution(String executionId) {
        FlowExecutionEntity ex = flowExecutionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new GatewayException("NOT_FOUND", "Execution not found"));
        ex.setStatus(ExecutionStatus.CANCELLED);
        ex.setCompletedAt(Instant.now());
        flowExecutionRepository.save(ex);
        executionWebSocketHandler.broadcast(executionId,
                ExecutionTraceEvent.flowFailed(executionId, "cancelled"));
    }

    @Transactional(readOnly = true)
    public DashboardStats getExecutionStats() {
        long totalFlows = flowRepository.count();
        long activeFlows = flowRepository.countByStatus(com.lokesh.gateway.common.enums.FlowStatus.ACTIVE);
        long totalExecutions = flowExecutionRepository.count();
        long running = flowExecutionRepository.countByStatus(ExecutionStatus.RUNNING);
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long completedToday = flowExecutionRepository.countByStatusAndCreatedAtAfter(ExecutionStatus.COMPLETED, startOfDay);
        long failedToday = flowExecutionRepository.countByStatusAndCreatedAtAfter(ExecutionStatus.FAILED, startOfDay);
        long completedTotal = flowExecutionRepository.countByStatus(ExecutionStatus.COMPLETED);
        double successRate = totalExecutions > 0 ? (double) completedTotal / totalExecutions : 0.0;
        double avgDuration = flowRepository.findAll().stream()
                .mapToDouble(f -> f.getAvgDurationMs())
                .average()
                .orElse(0.0);
        List<CircuitBreakerStatus> circuits = circuitBreakerManager.getAllCircuits();
        return new DashboardStats(
                totalFlows,
                activeFlows,
                totalExecutions,
                running,
                completedToday,
                failedToday,
                successRate,
                avgDuration,
                circuits
        );
    }

    private FlowExecutionResponse toResponse(FlowExecutionEntity ex, boolean includeSteps) {
        List<StepExecutionResponse> se = includeSteps
                ? stepExecutionRepository.findByFlowExecution_IdOrderByCreatedAtAsc(ex.getId()).stream()
                .map(this::toStepResponse)
                .toList()
                : List.of();
        return new FlowExecutionResponse(
                ex.getId().toString(),
                ex.getExecutionId(),
                ex.getFlow().getId().toString(),
                ex.getFlowName(),
                ex.getStatus(),
                readMap(ex.getInputData()),
                readMap(ex.getOutputData()),
                ex.getCorrelationId(),
                ex.getStartedAt(),
                ex.getCompletedAt(),
                ex.getDurationMs(),
                se,
                ex.getCreatedAt()
        );
    }

    private StepExecutionResponse toStepResponse(StepExecutionEntity s) {
        return new StepExecutionResponse(
                s.getId().toString(),
                s.getStepId(),
                s.getStepName(),
                s.getStepType(),
                s.getStatus(),
                readMap(s.getInputData()),
                readMap(s.getOutputData()),
                s.getErrorMessage(),
                s.getAttempt(),
                s.getStartedAt(),
                s.getCompletedAt(),
                s.getDurationMs()
        );
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}

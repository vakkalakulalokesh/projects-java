package com.lokesh.gateway.service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.gateway.common.dto.FlowCreateRequest;
import com.lokesh.gateway.common.dto.FlowDefinition;
import com.lokesh.gateway.common.dto.FlowResponse;
import com.lokesh.gateway.common.dto.FlowTriggerRequest;
import com.lokesh.gateway.common.enums.ExecutionStatus;
import com.lokesh.gateway.common.enums.FlowStatus;
import com.lokesh.gateway.common.exception.GatewayException;
import com.lokesh.gateway.service.engine.FlowOrchestrator;
import com.lokesh.gateway.service.engine.FlowValidator;
import com.lokesh.gateway.service.entity.FlowEntity;
import com.lokesh.gateway.service.entity.FlowExecutionEntity;
import com.lokesh.gateway.service.repository.FlowExecutionRepository;
import com.lokesh.gateway.service.repository.FlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlowService {

    private final FlowRepository flowRepository;
    private final FlowExecutionRepository flowExecutionRepository;
    private final FlowValidator flowValidator;
    private final FlowOrchestrator flowOrchestrator;
    private final ObjectMapper objectMapper;

    @Transactional
    public FlowResponse createFlow(FlowCreateRequest request, String createdBy) {
        List<String> errors = flowValidator.validateFlow(request.getFlowDefinition());
        if (!errors.isEmpty()) {
            throw new GatewayException("VALIDATION_FAILED", String.join("; ", errors));
        }
        flowRepository.findByName(request.getName()).ifPresent(f -> {
            throw new GatewayException("DUPLICATE_NAME", "Flow name already exists");
        });
        FlowEntity entity = FlowEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .version(1)
                .flowDefinition(writeJson(request.getFlowDefinition()))
                .inputSchema(request.getInputSchema() != null ? writeJson(request.getInputSchema()) : null)
                .status(FlowStatus.DRAFT)
                .tags(writeJson(request.getTags()))
                .createdBy(createdBy != null ? createdBy : "system")
                .build();
        return toResponse(flowRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public FlowResponse getFlow(UUID id) {
        return toResponse(flowRepository.findById(id).orElseThrow(() -> notFound(id)));
    }

    @Transactional(readOnly = true)
    public Page<FlowResponse> listFlows(FlowStatus status, String search, int page, int size) {
        Pageable p = PageRequest.of(page, size);
        Page<FlowEntity> result;
        String q = search != null ? search : "";
        if (status != null) {
            result = q.isBlank()
                    ? flowRepository.findByStatus(status, p)
                    : flowRepository.findByStatusAndNameContainingIgnoreCase(status, q, p);
        } else {
            result = q.isBlank()
                    ? flowRepository.findAll(p)
                    : flowRepository.findByNameContainingIgnoreCase(q, p);
        }
        return result.map(this::toResponse);
    }

    @Transactional
    public FlowResponse updateFlow(UUID id, FlowCreateRequest request) {
        FlowEntity entity = flowRepository.findById(id).orElseThrow(() -> notFound(id));
        if (entity.getStatus() == FlowStatus.DISABLED) {
            throw new GatewayException("FLOW_DISABLED", "Flow is disabled");
        }
        List<String> errors = flowValidator.validateFlow(request.getFlowDefinition());
        if (!errors.isEmpty()) {
            throw new GatewayException("VALIDATION_FAILED", String.join("; ", errors));
        }
        entity.setDescription(request.getDescription());
        entity.setFlowDefinition(writeJson(request.getFlowDefinition()));
        entity.setInputSchema(request.getInputSchema() != null ? writeJson(request.getInputSchema()) : null);
        entity.setTags(writeJson(request.getTags()));
        entity.setVersion(entity.getVersion() + 1);
        return toResponse(flowRepository.save(entity));
    }

    @Transactional
    public void deleteFlow(UUID id) {
        FlowEntity entity = flowRepository.findById(id).orElseThrow(() -> notFound(id));
        entity.setStatus(FlowStatus.DISABLED);
        flowRepository.save(entity);
    }

    @Transactional
    public FlowExecutionEntity triggerFlow(UUID flowId, FlowTriggerRequest trigger) {
        FlowEntity flow = flowRepository.findById(flowId).orElseThrow(() -> notFound(flowId));
        if (flow.getStatus() != FlowStatus.ACTIVE) {
            throw new GatewayException("FLOW_NOT_ACTIVE", "Flow must be ACTIVE to trigger");
        }
        String executionId = UUID.randomUUID().toString();
        FlowExecutionEntity ex = FlowExecutionEntity.builder()
                .executionId(executionId)
                .flow(flow)
                .flowName(flow.getName())
                .status(ExecutionStatus.PENDING)
                .inputData(writeJson(trigger.getInputData()))
                .correlationId(trigger.getCorrelationId())
                .callbackUrl(trigger.getCallbackUrl())
                .build();
        ex = flowExecutionRepository.save(ex);
        flowOrchestrator.startExecution(ex.getId());
        return ex;
    }

    public List<String> validateFlow(FlowDefinition definition) {
        return flowValidator.validateFlow(definition);
    }

    @Transactional
    public FlowResponse activateFlow(UUID id) {
        FlowEntity entity = flowRepository.findById(id).orElseThrow(() -> notFound(id));
        if (entity.getStatus() == FlowStatus.DISABLED) {
            throw new GatewayException("FLOW_DISABLED", "Cannot activate disabled flow");
        }
        entity.setStatus(FlowStatus.ACTIVE);
        return toResponse(flowRepository.save(entity));
    }

    private FlowResponse toResponse(FlowEntity e) {
        try {
            FlowDefinition def = objectMapper.readValue(e.getFlowDefinition(), FlowDefinition.class);
            Map<String, Object> schema = e.getInputSchema() != null
                    ? objectMapper.readValue(e.getInputSchema(), new TypeReference<>() {
            })
                    : null;
            List<String> tags = e.getTags() != null
                    ? objectMapper.readValue(e.getTags(), new TypeReference<>() {
            })
                    : List.of();
            double successRate = e.getTotalExecutions() > 0
                    ? (double) e.getSuccessfulExecutions() / e.getTotalExecutions()
                    : 0.0;
            return new FlowResponse(
                    e.getId().toString(),
                    e.getName(),
                    e.getDescription(),
                    e.getVersion(),
                    def,
                    schema,
                    e.getStatus(),
                    tags,
                    e.getTotalExecutions(),
                    e.getAvgDurationMs(),
                    successRate,
                    e.getCreatedBy(),
                    e.getCreatedAt(),
                    e.getUpdatedAt()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to map flow", ex);
        }
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static GatewayException notFound(UUID id) {
        return new GatewayException("NOT_FOUND", "Flow not found: " + id);
    }
}

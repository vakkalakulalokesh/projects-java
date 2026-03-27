package com.lokesh.gateway.service.controller;

import com.lokesh.gateway.common.dto.FlowCreateRequest;
import com.lokesh.gateway.common.dto.FlowDefinition;
import com.lokesh.gateway.common.dto.FlowResponse;
import com.lokesh.gateway.common.dto.FlowTriggerRequest;
import com.lokesh.gateway.common.enums.FlowStatus;
import com.lokesh.gateway.service.entity.FlowExecutionEntity;
import com.lokesh.gateway.service.service.ExecutionService;
import com.lokesh.gateway.service.service.FlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flows")
@RequiredArgsConstructor
@Tag(name = "Flows")
public class FlowController {

    private final FlowService flowService;
    private final ExecutionService executionService;

    @PostMapping
    @Operation(summary = "Create flow")
    public ResponseEntity<FlowResponse> create(@Valid @RequestBody FlowCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flowService.createFlow(request, "system"));
    }

    @GetMapping
    @Operation(summary = "List flows")
    public Page<FlowResponse> list(
            @RequestParam(required = false) FlowStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return flowService.listFlows(status, search, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get flow by id")
    public FlowResponse get(@PathVariable UUID id) {
        return flowService.getFlow(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update flow")
    public FlowResponse update(@PathVariable UUID id, @Valid @RequestBody FlowCreateRequest request) {
        return flowService.updateFlow(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete flow (DISABLED)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        flowService.deleteFlow(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "Trigger flow execution")
    public ResponseEntity<?> trigger(@PathVariable UUID id, @RequestBody(required = false) FlowTriggerRequest request) {
        FlowTriggerRequest body = request != null ? request : FlowTriggerRequest.builder().build();
        FlowExecutionEntity ex = flowService.triggerFlow(id, body);
        return ResponseEntity.accepted().body(executionService.getExecution(ex.getExecutionId()));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate flow definition")
    public Map<String, Object> validate(@Valid @RequestBody FlowDefinition definition) {
        List<String> errors = flowService.validateFlow(definition);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valid", errors.isEmpty());
        body.put("errors", errors);
        return body;
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Set flow status to ACTIVE")
    public FlowResponse activate(@PathVariable UUID id) {
        return flowService.activateFlow(id);
    }
}

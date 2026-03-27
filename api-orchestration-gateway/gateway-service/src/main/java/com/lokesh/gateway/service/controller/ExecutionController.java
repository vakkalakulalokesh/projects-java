package com.lokesh.gateway.service.controller;

import com.lokesh.gateway.common.dto.DashboardStats;
import com.lokesh.gateway.common.dto.FlowExecutionResponse;
import com.lokesh.gateway.common.enums.ExecutionStatus;
import com.lokesh.gateway.service.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@Tag(name = "Executions")
public class ExecutionController {

    private final ExecutionService executionService;

    @GetMapping
    @Operation(summary = "List executions")
    public Page<FlowExecutionResponse> list(
            @RequestParam(required = false) UUID flowId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return executionService.listExecutions(flowId, status, page, size);
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get execution with steps")
    public FlowExecutionResponse get(@PathVariable String executionId) {
        return executionService.getExecution(executionId);
    }

    @PostMapping("/{executionId}/cancel")
    @Operation(summary = "Cancel execution")
    public ResponseEntity<Void> cancel(@PathVariable String executionId) {
        executionService.cancelExecution(executionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics")
    public DashboardStats stats() {
        return executionService.getExecutionStats();
    }
}

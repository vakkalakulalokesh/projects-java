package com.lokesh.jobscheduler.service.controller;

import com.lokesh.jobscheduler.common.dto.DashboardStats;
import com.lokesh.jobscheduler.common.dto.ExecutionResponse;
import com.lokesh.jobscheduler.common.enums.ExecutionStatus;
import com.lokesh.jobscheduler.service.service.ExecutionService;
import com.lokesh.jobscheduler.service.service.WorkerRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/executions")
@RequiredArgsConstructor
@Tag(name = "Executions")
public class ExecutionController {

    private final ExecutionService executionService;
    private final WorkerRegistryService workerRegistryService;

    @GetMapping
    @Operation(summary = "List executions")
    public Page<ExecutionResponse> list(
            @PathVariable String tenantId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return executionService.listExecutions(tenantId, jobId, status, PageRequest.of(page, size));
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get execution")
    public ExecutionResponse get(@PathVariable String tenantId, @PathVariable String executionId) {
        ExecutionResponse response = executionService.getExecution(executionId);
        if (!tenantId.equals(response.tenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Execution belongs to another tenant");
        }
        return response;
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics")
    public DashboardStats stats(@PathVariable String tenantId) {
        DashboardStats base = executionService.getDashboardStats(tenantId);
        return executionService.withWorkerCount(base, workerRegistryService.getWorkerCount());
    }
}

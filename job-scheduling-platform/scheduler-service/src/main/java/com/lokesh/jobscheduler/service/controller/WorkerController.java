package com.lokesh.jobscheduler.service.controller;

import com.lokesh.jobscheduler.common.dto.WorkerHeartbeat;
import com.lokesh.jobscheduler.service.service.WorkerRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
@Tag(name = "Workers")
public class WorkerController {

    private final WorkerRegistryService workerRegistryService;

    @GetMapping
    @Operation(summary = "List active workers")
    public List<WorkerHeartbeat> list() {
        return workerRegistryService.getActiveWorkers();
    }

    @GetMapping("/count")
    @Operation(summary = "Active worker count")
    public WorkerCountResponse count() {
        return new WorkerCountResponse(workerRegistryService.getWorkerCount());
    }

    public record WorkerCountResponse(long count) {
    }
}

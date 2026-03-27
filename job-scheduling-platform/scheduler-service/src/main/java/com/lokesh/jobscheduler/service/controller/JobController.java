package com.lokesh.jobscheduler.service.controller;

import com.lokesh.jobscheduler.common.dto.ExecutionResponse;
import com.lokesh.jobscheduler.common.dto.JobCreateRequest;
import com.lokesh.jobscheduler.common.dto.JobResponse;
import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.service.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create job")
    public JobResponse create(
            @PathVariable String tenantId,
            @Valid @RequestBody JobCreateRequest request) {
        request.setTenantId(tenantId);
        return jobService.createJob(request);
    }

    @GetMapping
    @Operation(summary = "List jobs")
    public Page<JobResponse> list(
            @PathVariable String tenantId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobService.listJobs(tenantId, status, PageRequest.of(page, size));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job")
    public JobResponse get(@PathVariable String tenantId, @PathVariable UUID jobId) {
        return jobService.getJob(tenantId, jobId);
    }

    @PutMapping("/{jobId}")
    @Operation(summary = "Update job")
    public JobResponse update(
            @PathVariable String tenantId,
            @PathVariable UUID jobId,
            @Valid @RequestBody JobCreateRequest request) {
        request.setTenantId(tenantId);
        return jobService.updateJob(tenantId, jobId, request);
    }

    @DeleteMapping("/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete job (soft disable)")
    public void delete(@PathVariable String tenantId, @PathVariable UUID jobId) {
        jobService.deleteJob(tenantId, jobId);
    }

    @PostMapping("/{jobId}/trigger")
    @Operation(summary = "Trigger job manually")
    public ExecutionResponse trigger(@PathVariable String tenantId, @PathVariable UUID jobId) {
        return jobService.triggerJob(tenantId, jobId);
    }

    @PostMapping("/{jobId}/pause")
    @Operation(summary = "Pause job")
    public JobResponse pause(@PathVariable String tenantId, @PathVariable UUID jobId) {
        return jobService.pauseJob(tenantId, jobId);
    }

    @PostMapping("/{jobId}/resume")
    @Operation(summary = "Resume job")
    public JobResponse resume(@PathVariable String tenantId, @PathVariable UUID jobId) {
        return jobService.resumeJob(tenantId, jobId);
    }
}

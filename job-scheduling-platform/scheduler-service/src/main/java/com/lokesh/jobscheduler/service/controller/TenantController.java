package com.lokesh.jobscheduler.service.controller;

import com.lokesh.jobscheduler.common.dto.TenantInfo;
import com.lokesh.jobscheduler.service.dto.TenantCreateRequest;
import com.lokesh.jobscheduler.service.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create tenant")
    public TenantInfo create(@Valid @RequestBody TenantCreateRequest request) {
        return tenantService.createTenant(request);
    }

    @GetMapping
    @Operation(summary = "List tenants")
    public List<TenantInfo> list() {
        return tenantService.listTenants();
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Get tenant")
    public TenantInfo get(@PathVariable String tenantId) {
        return tenantService.getTenant(tenantId);
    }
}

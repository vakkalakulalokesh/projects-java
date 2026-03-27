package com.lokesh.jobscheduler.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantCreateRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String name;

    @Builder.Default
    private int executionQuota = 1000;
}

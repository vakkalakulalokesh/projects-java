package com.lokesh.jobscheduler.common.dto;

import com.lokesh.jobscheduler.common.enums.JobType;
import com.lokesh.jobscheduler.common.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreateRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private JobType jobType;

    private Map<String, Object> configuration;

    private String cronExpression;

    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Builder.Default
    private int timeoutSeconds = 300;

    @Builder.Default
    private int maxRetries = 3;

    private List<String> tags;
}

package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetryConfig {

    @Builder.Default
    private int maxAttempts = 3;

    @Builder.Default
    private String strategy = "fixed";

    @Builder.Default
    private long delayMs = 1000L;

    @Builder.Default
    private long maxDelayMs = 30000L;
}

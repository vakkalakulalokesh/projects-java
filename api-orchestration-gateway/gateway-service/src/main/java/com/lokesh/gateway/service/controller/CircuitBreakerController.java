package com.lokesh.gateway.service.controller;

import com.lokesh.gateway.common.dto.CircuitBreakerStatus;
import com.lokesh.gateway.service.service.CircuitBreakerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/circuit-breakers")
@RequiredArgsConstructor
@Tag(name = "Circuit breakers")
public class CircuitBreakerController {

    private final CircuitBreakerService circuitBreakerService;

    @GetMapping
    @Operation(summary = "List all circuit breakers")
    public List<CircuitBreakerStatus> list() {
        return circuitBreakerService.getAll();
    }

    @GetMapping("/{endpoint}")
    @Operation(summary = "Get circuit by endpoint key")
    public CircuitBreakerStatus get(@PathVariable String endpoint) {
        return circuitBreakerService.getByEndpoint(URLDecoder.decode(endpoint, StandardCharsets.UTF_8));
    }

    @PostMapping("/{endpoint}/reset")
    @Operation(summary = "Reset circuit to CLOSED")
    public ResponseEntity<Void> reset(@PathVariable String endpoint) {
        circuitBreakerService.resetCircuit(URLDecoder.decode(endpoint, StandardCharsets.UTF_8));
        return ResponseEntity.noContent().build();
    }
}

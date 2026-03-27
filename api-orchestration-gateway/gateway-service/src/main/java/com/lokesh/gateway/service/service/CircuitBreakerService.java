package com.lokesh.gateway.service.service;

import com.lokesh.gateway.common.dto.CircuitBreakerStatus;
import com.lokesh.gateway.service.engine.CircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CircuitBreakerService {

    private final CircuitBreakerManager circuitBreakerManager;

    public List<CircuitBreakerStatus> getAll() {
        return circuitBreakerManager.getAllCircuits();
    }

    public CircuitBreakerStatus getByEndpoint(String endpoint) {
        return circuitBreakerManager.getCircuitStatus(endpoint);
    }

    public void resetCircuit(String endpoint) {
        circuitBreakerManager.resetCircuit(endpoint);
    }
}

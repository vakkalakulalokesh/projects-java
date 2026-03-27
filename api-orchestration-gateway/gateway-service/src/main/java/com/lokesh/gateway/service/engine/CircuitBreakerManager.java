package com.lokesh.gateway.service.engine;

import com.lokesh.gateway.common.dto.CircuitBreakerStatus;
import com.lokesh.gateway.common.enums.CircuitState;
import com.lokesh.gateway.common.exception.GatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CircuitBreakerManager {

    private static final String CB_PREFIX = "gw:cb:";
    private static final String INDEX_KEY = "gw:cb:index";
    private static final int FAILURE_THRESHOLD = 5;
    private static final long OPEN_SECONDS = 30;

    private final StringRedisTemplate stringRedisTemplate;

    public void checkCircuit(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        String ep = normalize(endpoint);
        CircuitState state = CircuitState.valueOf(
                Objects.requireNonNullElse(stringRedisTemplate.opsForHash().get(key(ep), "state"), "CLOSED").toString());
        if (state == CircuitState.CLOSED) {
            return;
        }
        if (state == CircuitState.OPEN) {
            String openUntil = Objects.toString(stringRedisTemplate.opsForHash().get(key(ep), "openUntil"), null);
            if (openUntil != null) {
                Instant until = Instant.parse(openUntil);
                if (Instant.now().isBefore(until)) {
                    throw new GatewayException("CIRCUIT_OPEN", "Circuit open for endpoint: " + ep);
                }
                transitionHalfOpen(ep);
            }
            return;
        }
        if (state == CircuitState.HALF_OPEN) {
            String halfLock = Objects.toString(stringRedisTemplate.opsForHash().get(key(ep), "halfProbe"), "0");
            if (!"1".equals(halfLock)) {
                stringRedisTemplate.opsForHash().put(key(ep), "halfProbe", "1");
            }
        }
    }

    public void recordSuccess(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        String ep = normalize(endpoint);
        touchIndex(ep);
        StringRedisTemplate ops = stringRedisTemplate;
        String k = key(ep);
        CircuitState state = CircuitState.valueOf(
                Objects.requireNonNullElse(ops.opsForHash().get(k, "state"), "CLOSED").toString());
        ops.opsForHash().increment(k, "successCount", 1L);
        ops.opsForHash().put(k, "lastSuccessAt", Instant.now().toString());
        if (state == CircuitState.HALF_OPEN || state == CircuitState.OPEN) {
            ops.opsForHash().put(k, "state", CircuitState.CLOSED.name());
            ops.opsForHash().put(k, "failureCount", "0");
            ops.opsForHash().put(k, "halfProbe", "0");
            ops.opsForHash().delete(k, "openUntil");
        }
    }

    public void recordFailure(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        String ep = normalize(endpoint);
        touchIndex(ep);
        String k = key(ep);
        Long failures = stringRedisTemplate.opsForHash().increment(k, "failureCount", 1L);
        stringRedisTemplate.opsForHash().put(k, "lastFailureAt", Instant.now().toString());
        if (failures != null && failures > FAILURE_THRESHOLD) {
            Instant until = Instant.now().plusSeconds(OPEN_SECONDS);
            stringRedisTemplate.opsForHash().put(k, "state", CircuitState.OPEN.name());
            stringRedisTemplate.opsForHash().put(k, "openUntil", until.toString());
            stringRedisTemplate.opsForHash().put(k, "halfProbe", "0");
        }
    }

    public CircuitBreakerStatus getCircuitStatus(String endpoint) {
        String ep = normalize(endpoint);
        String k = key(ep);
        var entries = stringRedisTemplate.opsForHash().entries(k);
        if (entries.isEmpty()) {
            return new CircuitBreakerStatus(ep, CircuitState.CLOSED, 0, 0, null, null, null);
        }
        CircuitState state = CircuitState.valueOf(Objects.toString(entries.get("state"), "CLOSED"));
        int fc = parseInt(entries.get("failureCount"));
        int sc = parseInt(entries.get("successCount"));
        Instant lastFail = parseInstant(entries.get("lastFailureAt"));
        Instant lastOk = parseInstant(entries.get("lastSuccessAt"));
        Instant nextRetry = state == CircuitState.OPEN ? parseInstant(entries.get("openUntil")) : null;
        return new CircuitBreakerStatus(ep, state, fc, sc, lastFail, lastOk, nextRetry);
    }

    public List<CircuitBreakerStatus> getAllCircuits() {
        Set<String> eps = stringRedisTemplate.opsForSet().members(INDEX_KEY);
        if (eps == null || eps.isEmpty()) {
            return List.of();
        }
        List<CircuitBreakerStatus> list = new ArrayList<>();
        for (String ep : eps) {
            list.add(getCircuitStatus(ep));
        }
        return list;
    }

    public void resetCircuit(String endpoint) {
        String ep = normalize(endpoint);
        stringRedisTemplate.delete(key(ep));
        stringRedisTemplate.opsForSet().remove(INDEX_KEY, ep);
    }

    private void transitionHalfOpen(String ep) {
        stringRedisTemplate.opsForHash().put(key(ep), "state", CircuitState.HALF_OPEN.name());
        stringRedisTemplate.opsForHash().put(key(ep), "halfProbe", "0");
        stringRedisTemplate.opsForHash().delete(key(ep), "openUntil");
    }

    private void touchIndex(String ep) {
        stringRedisTemplate.opsForSet().add(INDEX_KEY, ep);
    }

    private static String key(String ep) {
        return CB_PREFIX + ep;
    }

    private static String normalize(String endpoint) {
        return endpoint.trim().toLowerCase();
    }

    private static int parseInt(Object o) {
        if (o == null) {
            return 0;
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Instant parseInstant(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Instant.parse(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}

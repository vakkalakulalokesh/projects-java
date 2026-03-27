package com.lokesh.gateway.service.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ExecutionContextService {

    private static final String KEY_PREFIX = "gw:ctx:";
    private static final long TTL_HOURS = 48;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void initContext(String executionId, Map<String, Object> input) {
        Map<String, Object> root = new HashMap<>();
        root.put("input", input != null ? input : Map.of());
        root.put("steps", new HashMap<String, Object>());
        write(executionId, root);
    }

    @SuppressWarnings("unchecked")
    public void putStepOutput(String executionId, String stepId, Map<String, Object> output) {
        Map<String, Object> ctx = read(executionId);
        if (ctx == null) {
            ctx = new HashMap<>();
            ctx.put("input", Map.of());
            ctx.put("steps", new HashMap<String, Object>());
        }
        Map<String, Object> steps = (Map<String, Object>) ctx.computeIfAbsent("steps", k -> new HashMap<String, Object>());
        steps.put(stepId, output != null ? output : Map.of());
        write(executionId, ctx);
    }

    public Map<String, Object> getContext(String executionId) {
        Map<String, Object> ctx = read(executionId);
        return ctx != null ? ctx : Map.of();
    }

    public void deleteContext(String executionId) {
        stringRedisTemplate.delete(KEY_PREFIX + executionId);
    }

    private void write(String executionId, Map<String, Object> ctx) {
        try {
            String json = objectMapper.writeValueAsString(ctx);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + executionId, json, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist execution context", e);
        }
    }

    private Map<String, Object> read(String executionId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + executionId);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}

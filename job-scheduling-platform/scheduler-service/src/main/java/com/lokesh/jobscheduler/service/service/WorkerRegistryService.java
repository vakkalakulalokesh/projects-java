package com.lokesh.jobscheduler.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.jobscheduler.common.dto.WorkerHeartbeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerRegistryService {

    private static final String KEY_PREFIX = "worker:hb:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void processHeartbeat(WorkerHeartbeat heartbeat) {
        try {
            String key = KEY_PREFIX + heartbeat.workerId();
            String json = objectMapper.writeValueAsString(heartbeat);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(30));
        } catch (Exception e) {
            log.warn("Failed to persist worker heartbeat", e);
        }
    }

    public List<WorkerHeartbeat> getActiveWorkers() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<WorkerHeartbeat> list = new ArrayList<>();
        for (String key : keys) {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                continue;
            }
            try {
                list.add(objectMapper.readValue(json, WorkerHeartbeat.class));
            } catch (Exception e) {
                log.debug("Skip invalid heartbeat payload at {}", key);
            }
        }
        return list;
    }

    public long getWorkerCount() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }

    @Scheduled(fixedRate = 10_000)
    public void cleanupStaleWorkers() {
    }
}

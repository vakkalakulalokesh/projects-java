package com.lokesh.gateway.service.rabbitmq.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import com.lokesh.gateway.service.engine.CircuitBreakerManager;
import com.lokesh.gateway.service.rabbitmq.StepResultSender;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HttpStepWorker {

    private final WebClient.Builder webClientBuilder;
    private final StepResultSender stepResultSender;
    private final CircuitBreakerManager circuitBreakerManager;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.Q_HTTP, concurrency = "3", containerFactory = "rabbitListenerContainerFactory")
    public void handle(StepDispatchMessage job) {
        long t0 = System.currentTimeMillis();
        String url = "";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> in = job.getInputData() != null ? job.getInputData() : Map.of();
            @SuppressWarnings("unchecked")
            Map<String, Object> rc = (Map<String, Object>) in.getOrDefault("resolvedConfig", Map.of());
            url = Objects.toString(rc.get("url"), "");
            if (url.isBlank()) {
                stepResultSender.sendFailure(job, "Missing url", elapsed(t0));
                return;
            }
            circuitBreakerManager.checkCircuit(url);
            HttpMethod method = HttpMethod.valueOf(Objects.toString(rc.getOrDefault("method", "GET"), "GET").toUpperCase());
            int timeout = Math.max(1, job.getTimeoutSeconds());
            WebClient client = webClientBuilder.build();

            var req = client.method(method).uri(url).headers(h -> applyHeaders(h, rc.get("headers")));
            Object body = rc.get("body");
            String raw;
            if (body != null && (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
                raw = req.contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body instanceof String s ? s : objectMapper.writeValueAsString(body))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(timeout));
            } else {
                raw = req.retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(timeout));
            }

            circuitBreakerManager.recordSuccess(url);
            Map<String, Object> output = extractOutput(raw, job.getConfig());
            stepResultSender.sendSuccess(job, output, elapsed(t0), null);
        } catch (WebClientResponseException | WebClientRequestException e) {
            if (!url.isBlank()) {
                circuitBreakerManager.recordFailure(url);
            }
            stepResultSender.sendFailure(job, e.getMessage(), elapsed(t0));
        } catch (Exception e) {
            if (!url.isBlank()) {
                circuitBreakerManager.recordFailure(url);
            }
            stepResultSender.sendFailure(job, e.getMessage() != null ? e.getMessage() : "http_error", elapsed(t0));
        }
    }

    @SuppressWarnings("unchecked")
    private void applyHeaders(HttpHeaders headers, Object hdrObj) {
        if (!(hdrObj instanceof Map<?, ?> m)) {
            return;
        }
        for (Map.Entry<?, ?> e : m.entrySet()) {
            headers.add(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractOutput(String raw, Map<String, Object> config) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("body", raw);
        if (config == null || raw == null || raw.isBlank()) {
            return output;
        }
        Object ext = config.get("extractors");
        if (!(ext instanceof Map<?, ?> em)) {
            return output;
        }
        Map<String, Object> fields = new HashMap<>();
        for (Map.Entry<?, ?> e : em.entrySet()) {
            try {
                fields.put(String.valueOf(e.getKey()), JsonPath.read(raw, String.valueOf(e.getValue())));
            } catch (Exception ignored) {
                fields.put(String.valueOf(e.getKey()), null);
            }
        }
        output.put("extracted", fields);
        return output;
    }

    private static long elapsed(long t0) {
        return System.currentTimeMillis() - t0;
    }
}

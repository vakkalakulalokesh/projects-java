package com.lokesh.jobscheduler.service.rabbitmq;

import com.lokesh.jobscheduler.common.dto.ExecutionLogEntry;
import com.lokesh.jobscheduler.common.enums.ExecutionStatus;
import com.lokesh.jobscheduler.common.enums.JobType;
import com.lokesh.jobscheduler.service.controller.WebSocketController;
import com.lokesh.jobscheduler.service.entity.ExecutionEntity;
import com.lokesh.jobscheduler.service.entity.JobEntity;
import com.lokesh.jobscheduler.service.repository.ExecutionRepository;
import com.lokesh.jobscheduler.service.repository.JobRepository;
import com.lokesh.jobscheduler.service.service.ExecutionService;
import com.lokesh.jobscheduler.service.service.WorkerActivityTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobWorkerConsumer {

    private final ExecutionRepository executionRepository;
    private final JobRepository jobRepository;
    private final ExecutionService executionService;
    private final JobMessageProducer jobMessageProducer;
    private final RestTemplate restTemplate;
    private final WebSocketController webSocketController;
    private final WorkerActivityTracker workerActivityTracker;

    @RabbitListener(queues = RabbitMqNames.QUEUE_JOB_WORK, concurrency = "3")
    public void processJob(JobExecutionMessage message) {
        String workerId = resolveWorkerId();
        Optional<ExecutionEntity> opt = executionRepository.findByExecutionId(message.getExecutionId());
        if (opt.isEmpty()) {
            return;
        }
        ExecutionEntity execution = opt.get();
        if (isTerminal(execution.getStatus())) {
            return;
        }
        JobEntity job = jobRepository.findById(message.getJobId()).orElse(null);
        if (job == null) {
            return;
        }
        workerActivityTracker.beginExecution();
        try {
            executionService.updateExecutionStatus(
                    message.getExecutionId(),
                    ExecutionStatus.RUNNING,
                    null,
                    null,
                    workerId,
                    null);
            logLine(message.getExecutionId(), "INFO", "Execution started", workerId);
            ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "job-exec-" + message.getExecutionId());
                t.setDaemon(true);
                return t;
            });
            Future<String> future = executor.submit(() -> runJob(message));
            String output;
            try {
                output = future.get(message.getTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                handleFailure(message, job, workerId, "Interrupted");
                return;
            } catch (TimeoutException e) {
                future.cancel(true);
                executionService.updateExecutionStatus(
                        message.getExecutionId(),
                        ExecutionStatus.TIMED_OUT,
                        null,
                        "Execution exceeded timeout of " + message.getTimeoutSeconds() + "s",
                        workerId,
                        null);
                logLine(message.getExecutionId(), "ERROR", "Timed out", workerId);
                executionService.incrementJobFailure(job);
                return;
            } catch (Exception e) {
                future.cancel(true);
                Throwable root = e.getCause() != null ? e.getCause() : e;
                handleFailure(message, job, workerId, root.getMessage());
                return;
            } finally {
                executor.shutdownNow();
            }
            ExecutionEntity latest = executionRepository.findByExecutionId(message.getExecutionId()).orElse(execution);
            executionService.updateExecutionStatus(
                    message.getExecutionId(),
                    ExecutionStatus.COMPLETED,
                    output,
                    null,
                    workerId,
                    durationSinceStart(latest));
            logLine(message.getExecutionId(), "INFO", "Execution completed", workerId);
            executionService.incrementJobSuccess(job);
        } catch (Exception e) {
            log.error("Worker failed for execution {}", message.getExecutionId(), e);
            handleFailure(message, job, workerId, e.getMessage());
        } finally {
            workerActivityTracker.endExecution();
        }
    }

    private void handleFailure(JobExecutionMessage message, JobEntity job, String workerId, String error) {
        ExecutionEntity ex = executionRepository.findByExecutionId(message.getExecutionId()).orElse(null);
        if (ex == null) {
            return;
        }
        if (ex.getAttempt() < ex.getMaxAttempts()) {
            int nextAttempt = ex.getAttempt() + 1;
            ex.setAttempt(nextAttempt);
            ex.setStatus(ExecutionStatus.RETRYING);
            ex.setErrorMessage(truncate(error, 4000));
            ex.setWorkerId(workerId);
            executionRepository.save(ex);
            executionService.updateExecutionStatus(
                    message.getExecutionId(),
                    ExecutionStatus.RETRYING,
                    null,
                    truncate(error, 4000),
                    workerId,
                    null);
            int exp = Math.min(nextAttempt - 1, 18);
            long delayMs = Math.min(600_000L, 1000L * (1L << exp));
            JobExecutionMessage retry = message.toBuilder().attempt(nextAttempt).build();
            jobMessageProducer.sendToRetryQueue(retry, delayMs);
            logLine(message.getExecutionId(), "WARN", "Scheduled retry attempt " + nextAttempt + " in " + delayMs + "ms", workerId);
            return;
        }
        executionService.updateExecutionStatus(
                message.getExecutionId(),
                ExecutionStatus.DEAD_LETTERED,
                null,
                truncate(error, 4000),
                workerId,
                null);
        jobMessageProducer.sendToDlq(message);
        if (job != null) {
            executionService.incrementJobFailure(job);
        }
        logLine(message.getExecutionId(), "ERROR", "Dead-lettered: " + error, workerId);
    }

    private String runJob(JobExecutionMessage message) throws Exception {
        Map<String, Object> cfg = Optional.ofNullable(message.getConfiguration()).orElse(Map.of());
        return switch (message.getJobType()) {
            case HTTP_CALL -> executeHttp(cfg);
            case SHELL_SCRIPT -> simulateShell(cfg);
            case SQL_QUERY -> simulateSql(cfg);
            case PYTHON_SCRIPT -> simulatePython(cfg);
        };
    }

    private String executeHttp(Map<String, Object> cfg) {
        String url = String.valueOf(cfg.getOrDefault("url", "https://httpbin.org/get"));
        String method = String.valueOf(cfg.getOrDefault("method", "GET")).toUpperCase();
        HttpHeaders headers = new HttpHeaders();
        Object rawHeaders = cfg.get("headers");
        if (rawHeaders instanceof Map<?, ?> map) {
            map.forEach((k, v) -> headers.add(String.valueOf(k), String.valueOf(v)));
        }
        String body = cfg.get("body") != null ? String.valueOf(cfg.get("body")) : null;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
        String respBody = response.getBody() != null ? response.getBody() : "";
        if (respBody.length() > 8000) {
            respBody = respBody.substring(0, 8000) + "...";
        }
        return "HTTP " + response.getStatusCode().value() + " " + responseBodySummary(respBody);
    }

    private static String responseBodySummary(String body) {
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    private String simulateShell(Map<String, Object> cfg) throws InterruptedException {
        String script = String.valueOf(cfg.getOrDefault("script", "echo hello"));
        int delay = ThreadLocalRandom.current().nextInt(200, 2000);
        Thread.sleep(delay);
        return "Simulated shell exit 0. Script: " + truncate(script, 200) + " durationMs=" + delay;
    }

    private String simulateSql(Map<String, Object> cfg) throws InterruptedException {
        String query = String.valueOf(cfg.getOrDefault("query", "SELECT 1"));
        int delay = ThreadLocalRandom.current().nextInt(100, 800);
        Thread.sleep(delay);
        return "Simulated SQL OK. Query: " + truncate(query, 200) + " rows=1 durationMs=" + delay;
    }

    private String simulatePython(Map<String, Object> cfg) throws InterruptedException {
        String script = String.valueOf(cfg.getOrDefault("script", "print('ok')"));
        int delay = ThreadLocalRandom.current().nextInt(150, 1200);
        Thread.sleep(delay);
        return "Simulated Python stdout: ok. Script: " + truncate(script, 200) + " durationMs=" + delay;
    }

    private void logLine(String executionId, String level, String text, String workerId) {
        ExecutionLogEntry entry = new ExecutionLogEntry(LocalDateTime.now(), level, text, workerId);
        webSocketController.sendLogEntry(executionId, entry);
    }

    private static String resolveWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + Thread.currentThread().getName();
        } catch (Exception e) {
            return "unknown-" + Thread.currentThread().getName();
        }
    }

    private static boolean isTerminal(ExecutionStatus status) {
        return status == ExecutionStatus.COMPLETED
                || status == ExecutionStatus.FAILED
                || status == ExecutionStatus.TIMED_OUT
                || status == ExecutionStatus.DEAD_LETTERED;
    }

    private static Long durationSinceStart(ExecutionEntity execution) {
        if (execution.getStartedAt() == null) {
            return null;
        }
        long ms = java.time.Duration.between(execution.getStartedAt(), LocalDateTime.now()).toMillis();
        return ms;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}

package com.lokesh.gateway.service.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.gateway.common.dto.EdgeDefinition;
import com.lokesh.gateway.common.dto.FlowDefinition;
import com.lokesh.gateway.common.dto.StepDefinition;
import com.lokesh.gateway.common.enums.ExecutionStatus;
import com.lokesh.gateway.common.enums.StepExecutionStatus;
import com.lokesh.gateway.common.enums.StepType;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import com.lokesh.gateway.service.dto.StepResultMessage;
import com.lokesh.gateway.service.entity.FlowEntity;
import com.lokesh.gateway.service.entity.FlowExecutionEntity;
import com.lokesh.gateway.service.entity.StepExecutionEntity;
import com.lokesh.gateway.service.repository.FlowExecutionRepository;
import com.lokesh.gateway.service.repository.FlowRepository;
import com.lokesh.gateway.service.repository.StepExecutionRepository;
import com.lokesh.gateway.service.rabbitmq.StepDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowOrchestrator {

    private final FlowExecutionRepository flowExecutionRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final FlowRepository flowRepository;
    private final StepDispatcher stepDispatcher;
    private final ExecutionContextService executionContextService;
    private final StepInputResolver stepInputResolver;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public void startExecution(UUID executionPk) {
        FlowExecutionEntity ex = flowExecutionRepository.findById(executionPk).orElseThrow();
        FlowEntity flow = flowRepository.findById(ex.getFlow().getId()).orElseThrow();
        FlowDefinition def = readDefinition(flow.getFlowDefinition());

        Map<String, StepDefinition> byId = def.getSteps().stream()
                .collect(Collectors.toMap(StepDefinition::getStepId, s -> s, (a, b) -> a));
        Map<String, Set<String>> preds = buildPreds(def);

        List<StepExecutionEntity> existing = stepExecutionRepository.findByFlowExecution_IdOrderByCreatedAtAsc(ex.getId());
        if (existing.isEmpty()) {
            for (StepDefinition sd : def.getSteps()) {
                int maxAttempts = sd.getRetryConfig() != null ? sd.getRetryConfig().getMaxAttempts() : 1;
                StepExecutionEntity se = StepExecutionEntity.builder()
                        .flowExecution(ex)
                        .stepId(sd.getStepId())
                        .stepName(sd.getName())
                        .stepType(sd.getType())
                        .status(StepExecutionStatus.PENDING)
                        .attempt(0)
                        .maxAttempts(Math.max(1, maxAttempts))
                        .build();
                stepExecutionRepository.save(se);
            }
        }

        Map<String, Object> input = readJsonMap(ex.getInputData());
        executionContextService.initContext(ex.getExecutionId(), input);

        ex.setStatus(ExecutionStatus.RUNNING);
        if (ex.getStartedAt() == null) {
            ex.setStartedAt(Instant.now());
        }
        flowExecutionRepository.save(ex);

        List<StepExecutionEntity> steps = stepExecutionRepository.findByFlowExecution_IdOrderByCreatedAtAsc(ex.getId());
        Map<String, StepExecutionEntity> stepEnt = steps.stream()
                .collect(Collectors.toMap(StepExecutionEntity::getStepId, s -> s, (a, b) -> a));

        for (String root : preds.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Map.Entry::getKey).toList()) {
            StepExecutionEntity se = stepEnt.get(root);
            if (se != null && se.getStatus() == StepExecutionStatus.PENDING) {
                dispatchStep(ex, byId.get(root), se, readContext(ex.getExecutionId()));
            }
        }
    }

    @Transactional
    public void handleStepResult(StepResultMessage msg) {
        FlowExecutionEntity ex = flowExecutionRepository.findById(msg.getFlowExecutionPk()).orElseThrow();
        StepExecutionEntity step = stepExecutionRepository
                .findByFlowExecutionAndStepId(ex, msg.getStepId())
                .orElseThrow();

        Instant now = Instant.now();
        if (msg.getStatus() == StepExecutionStatus.RUNNING) {
            return;
        }

        if (msg.getStatus() == StepExecutionStatus.FAILED) {
            step.setErrorMessage(msg.getErrorMessage());
            step.setCompletedAt(now);
            step.setDurationMs(msg.getDurationMs());
            int nextAttempt = step.getAttempt() + 1;
            if (nextAttempt <= step.getMaxAttempts()) {
                step.setAttempt(nextAttempt);
                step.setStatus(StepExecutionStatus.PENDING);
                step.setStartedAt(null);
                step.setCompletedAt(null);
                step.setDurationMs(null);
                stepExecutionRepository.save(step);
                FlowEntity flow = flowRepository.findById(ex.getFlow().getId()).orElseThrow();
                FlowDefinition def = readDefinition(flow.getFlowDefinition());
                StepDefinition sd = def.getSteps().stream()
                        .filter(s -> s.getStepId().equals(step.getStepId()))
                        .findFirst()
                        .orElseThrow();
                Map<String, Object> ctx = readContext(ex.getExecutionId());
                dispatchRetry(ex, sd, step, ctx);
                return;
            }
            step.setStatus(StepExecutionStatus.FAILED);
            step.setAttempt(nextAttempt);
            StepDefinition sdFailed = loadStepDef(ex, step.getStepId());
            if (sdFailed != null && sdFailed.getCompensationConfig() != null && !sdFailed.getCompensationConfig().isEmpty()) {
                stepExecutionRepository.save(step);
                startCompensation(ex.getExecutionId());
                return;
            }
            stepExecutionRepository.save(step);
            FlowEntity flow = flowRepository.findById(ex.getFlow().getId()).orElseThrow();
            FlowDefinition def = readDefinition(flow.getFlowDefinition());
            finalizeIfDone(ex, def);
            return;
        }

        if (msg.getStatus() == StepExecutionStatus.COMPLETED) {
            step.setStatus(StepExecutionStatus.COMPLETED);
            step.setOutputData(writeJson(msg.getOutputData()));
            step.setCompletedAt(now);
            step.setDurationMs(msg.getDurationMs());
            step.setErrorMessage(null);
        } else {
            step.setStatus(msg.getStatus());
        }
        stepExecutionRepository.save(step);

        FlowEntity flow = flowRepository.findById(ex.getFlow().getId()).orElseThrow();
        FlowDefinition def = readDefinition(flow.getFlowDefinition());

        if (msg.getStatus() == StepExecutionStatus.COMPLETED) {
            executionContextService.putStepOutput(ex.getExecutionId(),
                    msg.getStepId(),
                    msg.getOutputData() != null ? msg.getOutputData() : Map.of());
            Map<String, Object> ctx = readContext(ex.getExecutionId());

            if (msg.getStepType() == StepType.CONDITION && msg.getBranchTargetStepId() != null) {
                applyConditionSkips(ex, def, msg.getStepId(), msg.getBranchTargetStepId());
            }

            Map<String, StepExecutionEntity> byStep = loadStepMap(ex);
            Map<String, Set<String>> preds = buildPreds(def);

            for (StepDefinition sd : def.getSteps()) {
                StepExecutionEntity se = byStep.get(sd.getStepId());
                if (se == null || se.getStatus() != StepExecutionStatus.PENDING) {
                    continue;
                }
                if (!predsTerminal(preds.get(sd.getStepId()), byStep)) {
                    continue;
                }
                dispatchStep(ex, sd, se, ctx);
            }
        }

        finalizeIfDone(ex, def);
    }

    @Transactional
    public void startCompensation(String executionId) {
        FlowExecutionEntity ex = flowExecutionRepository.findByExecutionId(executionId).orElseThrow();
        ex.setStatus(ExecutionStatus.COMPENSATING);
        flowExecutionRepository.save(ex);

        List<StepExecutionEntity> completed = stepExecutionRepository.findByFlowExecution_IdOrderByCreatedAtAsc(ex.getId())
                .stream()
                .filter(s -> s.getStatus() == StepExecutionStatus.COMPLETED)
                .sorted((a, b) -> {
                    Instant ca = a.getCompletedAt() != null ? a.getCompletedAt() : Instant.EPOCH;
                    Instant cb = b.getCompletedAt() != null ? b.getCompletedAt() : Instant.EPOCH;
                    return cb.compareTo(ca);
                })
                .toList();

        WebClient client = webClientBuilder.build();
        for (StepExecutionEntity se : completed) {
            StepDefinition sd = loadStepDef(ex, se.getStepId());
            if (sd == null || sd.getCompensationConfig() == null) {
                continue;
            }
            Map<String, Object> comp = sd.getCompensationConfig();
            String url = Objects.toString(comp.get("url"), "");
            if (url.isBlank()) {
                continue;
            }
            String method = Objects.toString(comp.getOrDefault("method", "POST"), "POST").toUpperCase();
            se.setStatus(StepExecutionStatus.COMPENSATING);
            stepExecutionRepository.save(se);
            try {
                client.method(HttpMethod.valueOf(method))
                        .uri(url)
                        .retrieve()
                        .toBodilessEntity()
                        .block(Duration.ofSeconds(30));
                se.setStatus(StepExecutionStatus.COMPENSATED);
            } catch (Exception e) {
                se.setStatus(StepExecutionStatus.FAILED);
                se.setErrorMessage(e.getMessage());
            }
            stepExecutionRepository.save(se);
        }

        ex.setStatus(ExecutionStatus.FAILED);
        ex.setCompletedAt(Instant.now());
        if (ex.getStartedAt() != null) {
            ex.setDurationMs(Duration.between(ex.getStartedAt(), ex.getCompletedAt()).toMillis());
        }
        ex.setErrorMessage("Compensated after failure");
        flowExecutionRepository.save(ex);
        updateFlowStats(ex);
    }

    private void applyConditionSkips(FlowExecutionEntity ex, FlowDefinition def, String conditionStepId, String branchTarget) {
        StepDefinition cond = def.getSteps().stream()
                .filter(s -> s.getStepId().equals(conditionStepId))
                .findFirst()
                .orElse(null);
        if (cond == null || cond.getConfig() == null) {
            return;
        }
        String onTrue = Objects.toString(cond.getConfig().get("onTrue"), "");
        String onFalse = Objects.toString(cond.getConfig().get("onFalse"), "");
        String sibling = branchTarget.equals(onTrue) ? onFalse : onTrue;
        if (sibling.isBlank()) {
            return;
        }
        stepExecutionRepository.findByFlowExecutionAndStepId(ex, sibling).ifPresent(se -> {
            if (se.getStatus() == StepExecutionStatus.PENDING) {
                se.setStatus(StepExecutionStatus.SKIPPED);
                se.setCompletedAt(Instant.now());
                stepExecutionRepository.save(se);
            }
        });
    }

    private void finalizeIfDone(FlowExecutionEntity ex, FlowDefinition def) {
        FlowExecutionEntity current = flowExecutionRepository.findById(ex.getId()).orElseThrow();
        if (current.getCompletedAt() != null) {
            return;
        }
        List<StepExecutionEntity> steps = stepExecutionRepository.findByFlowExecution_IdOrderByCreatedAtAsc(ex.getId());
        boolean anyFailed = steps.stream().anyMatch(s -> s.getStatus() == StepExecutionStatus.FAILED);
        if (anyFailed && current.getStatus() != ExecutionStatus.COMPENSATING) {
            current.setStatus(ExecutionStatus.FAILED);
            current.setCompletedAt(Instant.now());
            if (current.getStartedAt() != null) {
                current.setDurationMs(Duration.between(current.getStartedAt(), current.getCompletedAt()).toMillis());
            }
            flowExecutionRepository.save(current);
            updateFlowStats(current);
            return;
        }
        Set<String> allIds = def.getSteps().stream().map(StepDefinition::getStepId).collect(Collectors.toSet());
        boolean allDone = allIds.stream().allMatch(id -> {
            StepExecutionEntity s = steps.stream().filter(x -> x.getStepId().equals(id)).findFirst().orElse(null);
            return s != null && terminal(s.getStatus());
        });
        if (allDone && !anyFailed) {
            current.setStatus(ExecutionStatus.COMPLETED);
            current.setCompletedAt(Instant.now());
            if (current.getStartedAt() != null) {
                current.setDurationMs(Duration.between(current.getStartedAt(), current.getCompletedAt()).toMillis());
            }
            try {
                Map<String, Object> ctx = readContext(current.getExecutionId());
                @SuppressWarnings("unchecked")
                Map<String, Object> outs = (Map<String, Object>) ctx.getOrDefault("steps", Map.of());
                current.setOutputData(objectMapper.writeValueAsString(outs));
            } catch (Exception e) {
                current.setOutputData("{}");
            }
            flowExecutionRepository.save(current);
            updateFlowStats(current);
        }
    }

    private static boolean terminal(StepExecutionStatus st) {
        return st == StepExecutionStatus.COMPLETED
                || st == StepExecutionStatus.SKIPPED
                || st == StepExecutionStatus.FAILED
                || st == StepExecutionStatus.COMPENSATED;
    }

    private boolean predsTerminal(Set<String> predIds, Map<String, StepExecutionEntity> byStep) {
        if (predIds == null || predIds.isEmpty()) {
            return true;
        }
        for (String p : predIds) {
            StepExecutionEntity se = byStep.get(p);
            if (se == null) {
                return false;
            }
            StepExecutionStatus st = se.getStatus();
            if (st != StepExecutionStatus.COMPLETED && st != StepExecutionStatus.SKIPPED) {
                return false;
            }
        }
        return true;
    }

    private Map<String, StepExecutionEntity> loadStepMap(FlowExecutionEntity ex) {
        return stepExecutionRepository.findByFlowExecution_IdOrderByCreatedAtAsc(ex.getId()).stream()
                .collect(Collectors.toMap(StepExecutionEntity::getStepId, s -> s, (a, b) -> a));
    }

    private StepDefinition loadStepDef(FlowExecutionEntity ex, String stepId) {
        FlowEntity flow = flowRepository.findById(ex.getFlow().getId()).orElseThrow();
        FlowDefinition def = readDefinition(flow.getFlowDefinition());
        return def.getSteps().stream().filter(s -> s.getStepId().equals(stepId)).findFirst().orElse(null);
    }

    private void dispatchStep(FlowExecutionEntity ex, StepDefinition sd, StepExecutionEntity se, Map<String, Object> ctx) {
        se.setStatus(StepExecutionStatus.RUNNING);
        se.setStartedAt(Instant.now());
        if (se.getAttempt() == 0) {
            se.setAttempt(1);
        }
        stepExecutionRepository.save(se);

        Map<String, Object> resolvedConfig = sd.getConfig() != null
                ? stepInputResolver.resolveInput(sd.getConfig(), ctx)
                : Map.of();
        Map<String, Object> inputPayload = new HashMap<>();
        inputPayload.put("flowInput", ctx.getOrDefault("input", Map.of()));
        inputPayload.put("resolvedConfig", resolvedConfig);

        StepDispatchMessage msg = StepDispatchMessage.builder()
                .flowExecutionPk(ex.getId())
                .executionId(ex.getExecutionId())
                .stepId(sd.getStepId())
                .stepName(sd.getName())
                .stepType(sd.getType())
                .config(sd.getConfig())
                .inputData(inputPayload)
                .retryConfig(sd.getRetryConfig())
                .compensationConfig(sd.getCompensationConfig())
                .timeoutSeconds(sd.getTimeoutSeconds())
                .build();
        stepDispatcher.dispatchStep(msg);
    }

    private void dispatchRetry(FlowExecutionEntity ex, StepDefinition sd, StepExecutionEntity se, Map<String, Object> ctx) {
        se.setStatus(StepExecutionStatus.RUNNING);
        se.setStartedAt(Instant.now());
        stepExecutionRepository.save(se);
        Map<String, Object> resolvedConfig = sd.getConfig() != null
                ? stepInputResolver.resolveInput(sd.getConfig(), ctx)
                : Map.of();
        Map<String, Object> inputPayload = new HashMap<>();
        inputPayload.put("flowInput", ctx.getOrDefault("input", Map.of()));
        inputPayload.put("resolvedConfig", resolvedConfig);

        StepDispatchMessage msg = StepDispatchMessage.builder()
                .flowExecutionPk(ex.getId())
                .executionId(ex.getExecutionId())
                .stepId(sd.getStepId())
                .stepName(sd.getName())
                .stepType(sd.getType())
                .config(sd.getConfig())
                .inputData(inputPayload)
                .retryConfig(sd.getRetryConfig())
                .compensationConfig(sd.getCompensationConfig())
                .timeoutSeconds(sd.getTimeoutSeconds())
                .build();
        stepDispatcher.dispatchRetry(msg);
    }

    private Map<String, Object> readContext(String executionId) {
        return executionContextService.getContext(executionId);
    }

    private FlowDefinition readDefinition(String json) {
        try {
            return objectMapper.readValue(json, FlowDefinition.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid flow definition", e);
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Set<String>> buildPreds(FlowDefinition def) {
        Set<String> ids = def.getSteps().stream().map(StepDefinition::getStepId).collect(Collectors.toSet());
        Map<String, Set<String>> preds = new HashMap<>();
        for (String id : ids) {
            preds.put(id, new HashSet<>());
        }
        List<EdgeDefinition> edges = def.getEdges() != null ? def.getEdges() : List.of();
        for (EdgeDefinition e : edges) {
            preds.get(e.getToStep()).add(e.getFromStep());
        }
        return preds;
    }

    private void updateFlowStats(FlowExecutionEntity ex) {
        FlowEntity flow = flowRepository.findById(ex.getFlow().getId()).orElseThrow();
        flow.setTotalExecutions(flow.getTotalExecutions() + 1);
        if (ex.getStatus() == ExecutionStatus.COMPLETED) {
            flow.setSuccessfulExecutions(flow.getSuccessfulExecutions() + 1);
        }
        if (ex.getDurationMs() != null) {
            double n = flow.getTotalExecutions();
            flow.setAvgDurationMs((flow.getAvgDurationMs() * (n - 1) + ex.getDurationMs()) / n);
        }
        flowRepository.save(flow);
    }
}

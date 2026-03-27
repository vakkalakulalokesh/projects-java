package com.lokesh.gateway.service.rabbitmq.workers;

import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import com.lokesh.gateway.service.engine.ExecutionContextService;
import com.lokesh.gateway.service.rabbitmq.StepResultSender;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AggregateStepWorker {

    private final StepResultSender stepResultSender;
    private final ExecutionContextService executionContextService;

    @RabbitListener(queues = RabbitMQConfig.Q_AGGREGATE, containerFactory = "rabbitListenerContainerFactory")
    public void handle(StepDispatchMessage job) {
        long t0 = System.currentTimeMillis();
        try {
            Map<String, Object> cfg = job.getConfig() != null ? job.getConfig() : Map.of();
            @SuppressWarnings("unchecked")
            List<String> waitFor = (List<String>) cfg.get("waitFor");
            String strategy = Objects.toString(cfg.getOrDefault("strategy", "merge"), "merge");
            Map<String, Object> ctx = executionContextService.getContext(job.getExecutionId());
            @SuppressWarnings("unchecked")
            Map<String, Object> steps = (Map<String, Object>) ctx.getOrDefault("steps", Map.of());

            Map<String, Object> merged = new LinkedHashMap<>();
            if (waitFor != null) {
                for (String sid : waitFor) {
                    Object out = steps.get(sid);
                    if (out instanceof Map<?, ?> m) {
                        if ("merge".equalsIgnoreCase(strategy)) {
                            for (Map.Entry<?, ?> e : m.entrySet()) {
                                merged.put(String.valueOf(e.getKey()), e.getValue());
                            }
                        } else {
                            merged.put(sid, out);
                        }
                    }
                }
            }
            stepResultSender.sendSuccess(job, merged, System.currentTimeMillis() - t0, null);
        } catch (Exception e) {
            stepResultSender.sendFailure(job, Objects.toString(e.getMessage(), "aggregate_error"), System.currentTimeMillis() - t0);
        }
    }
}

package com.lokesh.gateway.service.rabbitmq.workers;

import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import com.lokesh.gateway.service.rabbitmq.StepResultSender;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ScriptStepWorker {

    private final StepResultSender stepResultSender;

    @RabbitListener(queues = RabbitMQConfig.Q_SCRIPT, containerFactory = "rabbitListenerContainerFactory")
    public void handle(StepDispatchMessage job) {
        long t0 = System.currentTimeMillis();
        try {
            Map<String, Object> cfg = job.getConfig() != null ? job.getConfig() : Map.of();
            String language = Objects.toString(cfg.getOrDefault("language", "javascript"), "javascript");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("language", language);
            out.put("simulated", true);
            out.put("result", Map.of("ok", true, "message", "script step simulated"));
            stepResultSender.sendSuccess(job, out, System.currentTimeMillis() - t0, null);
        } catch (Exception e) {
            stepResultSender.sendFailure(job, Objects.toString(e.getMessage(), "script_error"), System.currentTimeMillis() - t0);
        }
    }
}

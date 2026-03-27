package com.lokesh.gateway.service.rabbitmq.workers;

import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import com.lokesh.gateway.service.rabbitmq.StepResultSender;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DelayStepWorker {

    private final StepResultSender stepResultSender;

    @RabbitListener(queues = RabbitMQConfig.Q_DELAY, containerFactory = "rabbitListenerContainerFactory")
    public void handle(StepDispatchMessage job) {
        long t0 = System.currentTimeMillis();
        try {
            Map<String, Object> cfg = job.getConfig() != null ? job.getConfig() : Map.of();
            long delay = ((Number) Objects.requireNonNullElse(cfg.get("delayMs"), 0L)).longValue();
            if (delay > 0) {
                Thread.sleep(Math.min(delay, 600_000L));
            }
            stepResultSender.sendSuccess(job, Map.of("delayedMs", delay), System.currentTimeMillis() - t0, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stepResultSender.sendFailure(job, "interrupted", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            stepResultSender.sendFailure(job, Objects.toString(e.getMessage(), "delay_error"), System.currentTimeMillis() - t0);
        }
    }
}

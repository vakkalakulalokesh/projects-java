package com.lokesh.gateway.service.rabbitmq.workers;

import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import com.lokesh.gateway.service.rabbitmq.StepResultSender;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ConditionStepWorker {

    private final StepResultSender stepResultSender;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    @RabbitListener(queues = RabbitMQConfig.Q_CONDITION, containerFactory = "rabbitListenerContainerFactory")
    public void handle(StepDispatchMessage job) {
        long t0 = System.currentTimeMillis();
        try {
            Map<String, Object> cfg = job.getConfig() != null ? job.getConfig() : Map.of();
            String condition = Objects.toString(cfg.get("condition"), "false");
            String onTrue = Objects.toString(cfg.get("onTrue"), "");
            String onFalse = Objects.toString(cfg.get("onFalse"), "");

            @SuppressWarnings("unchecked")
            Map<String, Object> in = job.getInputData() != null ? job.getInputData() : Map.of();
            @SuppressWarnings("unchecked")
            Map<String, Object> flowInput = (Map<String, Object>) in.getOrDefault("flowInput", Map.of());

            StandardEvaluationContext ctx = new StandardEvaluationContext();
            ctx.setVariable("input", flowInput);
            Boolean ok = spelParser.parseExpression(condition).getValue(ctx, Boolean.class);
            boolean branch = Boolean.TRUE.equals(ok);
            String target = branch ? onTrue : onFalse;

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("result", branch);
            output.put("branchTargetStepId", target);
            stepResultSender.sendSuccess(job, output, System.currentTimeMillis() - t0, target);
        } catch (Exception e) {
            stepResultSender.sendFailure(job, Objects.toString(e.getMessage(), "condition_error"), System.currentTimeMillis() - t0);
        }
    }
}

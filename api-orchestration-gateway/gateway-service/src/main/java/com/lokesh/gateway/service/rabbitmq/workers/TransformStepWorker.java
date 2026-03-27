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
public class TransformStepWorker {

    private final StepResultSender stepResultSender;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    @RabbitListener(queues = RabbitMQConfig.Q_TRANSFORM, containerFactory = "rabbitListenerContainerFactory")
    public void handle(StepDispatchMessage job) {
        long t0 = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> in = job.getInputData() != null ? job.getInputData() : Map.of();
            @SuppressWarnings("unchecked")
            Map<String, Object> flowInput = (Map<String, Object>) in.getOrDefault("flowInput", Map.of());
            Object mappingsObj = job.getConfig() != null ? job.getConfig().get("mappings") : null;
            Map<String, Object> output = new LinkedHashMap<>();
            if (mappingsObj instanceof Map<?, ?> mm) {
                StandardEvaluationContext ctx = new StandardEvaluationContext();
                ctx.setVariable("input", flowInput);
                for (Map.Entry<?, ?> e : mm.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    Object val = e.getValue();
                    if (val instanceof String expr && (expr.contains("*") || expr.contains("+") || expr.contains("-")
                            || expr.contains("/") || expr.contains("input."))) {
                        try {
                            output.put(key, spelParser.parseExpression(expr).getValue(ctx));
                        } catch (Exception ex) {
                            output.put(key, null);
                        }
                    } else if (val instanceof String path) {
                        output.put(key, resolvePath(flowInput, path));
                    } else {
                        output.put(key, val);
                    }
                }
            }
            stepResultSender.sendSuccess(job, output, System.currentTimeMillis() - t0, null);
        } catch (Exception e) {
            stepResultSender.sendFailure(job, Objects.toString(e.getMessage(), "transform_error"), System.currentTimeMillis() - t0);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object resolvePath(Map<String, Object> root, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object cur = root;
        int i = 0;
        if (parts.length > 0 && "input".equals(parts[0])) {
            i = 1;
        }
        for (; i < parts.length; i++) {
            if (!(cur instanceof Map<?, ?> m)) {
                return null;
            }
            cur = m.get(parts[i]);
        }
        return cur;
    }
}

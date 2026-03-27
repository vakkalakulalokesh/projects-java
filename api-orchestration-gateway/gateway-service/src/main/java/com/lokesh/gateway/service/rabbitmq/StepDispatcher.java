package com.lokesh.gateway.service.rabbitmq;

import com.lokesh.gateway.common.enums.StepType;
import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StepDispatcher {

    private final RabbitTemplate rabbitTemplate;

    public void dispatchStep(StepDispatchMessage message) {
        String rk = routingKeyFor(message.getStepType());
        rabbitTemplate.convertAndSend(RabbitMQConfig.STEP_EXECUTION_EXCHANGE, rk, message, m -> {
            if (message.getPriority() != null) {
                m.getMessageProperties().setPriority(message.getPriority());
            }
            m.getMessageProperties().setCorrelationId(message.getExecutionId());
            m.getMessageProperties().setHeader("gw-execution-id", message.getExecutionId());
            m.getMessageProperties().setHeader("gw-step-id", message.getStepId());
            return m;
        });
    }

    public void dispatchRetry(StepDispatchMessage message) {
        String rk = routingKeyFor(message.getStepType());
        rabbitTemplate.convertAndSend(RabbitMQConfig.STEP_RETRY_EXCHANGE, "retry", message, m -> {
            m.getMessageProperties().setHeader(RabbitMQConfig.HEADER_TARGET_ROUTING_KEY, rk);
            m.getMessageProperties().setCorrelationId(message.getExecutionId());
            return m;
        });
    }

    public static String routingKeyFor(StepType type) {
        return switch (type) {
            case HTTP_CALL -> "step.http";
            case TRANSFORM -> "step.transform";
            case CONDITION -> "step.condition";
            case DELAY -> "step.delay";
            case AGGREGATE -> "step.aggregate";
            case SCRIPT -> "step.script";
        };
    }
}

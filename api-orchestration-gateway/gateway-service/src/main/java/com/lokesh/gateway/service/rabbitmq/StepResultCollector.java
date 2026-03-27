package com.lokesh.gateway.service.rabbitmq;

import com.lokesh.gateway.common.dto.ExecutionTraceEvent;
import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepResultMessage;
import com.lokesh.gateway.service.engine.FlowOrchestrator;
import com.lokesh.gateway.service.websocket.ExecutionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StepResultCollector {

    private final FlowOrchestrator flowOrchestrator;
    private final ExecutionWebSocketHandler executionWebSocketHandler;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.STEP_RESULT_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void onResult(StepResultMessage msg) {
        flowOrchestrator.handleStepResult(msg);
        ExecutionTraceEvent trace = switch (msg.getStatus()) {
            case COMPLETED -> ExecutionTraceEvent.stepCompleted(
                    msg.getExecutionId(), msg.getStepId(), traceStepName(msg), msg.getOutputData());
            case FAILED -> ExecutionTraceEvent.stepFailed(
                    msg.getExecutionId(), msg.getStepId(), traceStepName(msg),
                    msg.getErrorMessage() != null ? msg.getErrorMessage() : "failed");
            default -> new ExecutionTraceEvent(
                    "STEP_UPDATE",
                    msg.getExecutionId(),
                    msg.getStepId(),
                    traceStepName(msg),
                    msg.getStatus(),
                    null,
                    null,
                    java.time.Instant.now());
        };
        executionWebSocketHandler.broadcast(msg.getExecutionId(), trace);
        rabbitTemplate.convertAndSend("execution.events", "", trace);
    }

    private static String traceStepName(StepResultMessage msg) {
        return msg.getStepName() != null ? msg.getStepName() : msg.getStepId();
    }
}

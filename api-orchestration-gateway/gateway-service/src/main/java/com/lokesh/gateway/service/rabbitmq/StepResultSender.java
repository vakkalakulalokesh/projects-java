package com.lokesh.gateway.service.rabbitmq;

import com.lokesh.gateway.common.enums.StepExecutionStatus;
import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import com.lokesh.gateway.service.dto.StepResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StepResultSender {

    private final RabbitTemplate rabbitTemplate;

    public void sendSuccess(StepDispatchMessage job, Map<String, Object> output, long durationMs, String branchTargetStepId) {
        StepResultMessage result = StepResultMessage.builder()
                .flowExecutionPk(job.getFlowExecutionPk())
                .executionId(job.getExecutionId())
                .stepId(job.getStepId())
                .stepName(job.getStepName())
                .stepType(job.getStepType())
                .status(StepExecutionStatus.COMPLETED)
                .outputData(output)
                .durationMs(durationMs)
                .branchTargetStepId(branchTargetStepId)
                .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.STEP_RESULTS_EXCHANGE, "result", result);
    }

    public void sendFailure(StepDispatchMessage job, String error, long durationMs) {
        StepResultMessage result = StepResultMessage.builder()
                .flowExecutionPk(job.getFlowExecutionPk())
                .executionId(job.getExecutionId())
                .stepId(job.getStepId())
                .stepName(job.getStepName())
                .stepType(job.getStepType())
                .status(StepExecutionStatus.FAILED)
                .errorMessage(error)
                .durationMs(durationMs)
                .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.STEP_RESULTS_EXCHANGE, "result", result);
    }
}

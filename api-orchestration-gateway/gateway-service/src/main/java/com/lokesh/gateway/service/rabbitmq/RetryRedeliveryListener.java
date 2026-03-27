package com.lokesh.gateway.service.rabbitmq;

import com.lokesh.gateway.service.config.RabbitMQConfig;
import com.lokesh.gateway.service.dto.StepDispatchMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetryRedeliveryListener {

    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.STEP_REDELIVERY_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void onRedeliver(StepDispatchMessage message,
                            @Header(name = RabbitMQConfig.HEADER_TARGET_ROUTING_KEY, required = false) String targetRk) {
        if (targetRk == null || targetRk.isBlank()) {
            return;
        }
        rabbitTemplate.convertAndSend(RabbitMQConfig.STEP_EXECUTION_EXCHANGE, targetRk, message, m -> {
            m.getMessageProperties().setCorrelationId(message.getExecutionId());
            return m;
        });
    }
}

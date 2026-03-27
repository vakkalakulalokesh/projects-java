package com.lokesh.jobscheduler.service.rabbitmq;

import com.lokesh.jobscheduler.common.event.ExecutionEvent;
import com.lokesh.jobscheduler.service.controller.WebSocketController;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionEventConsumer {

    private final WebSocketController webSocketController;

    @RabbitListener(queues = RabbitMqNames.QUEUE_JOB_EVENTS)
    public void forwardToWebSocket(ExecutionEvent event) {
        webSocketController.sendExecutionEvent(event.tenantId(), event);
    }
}

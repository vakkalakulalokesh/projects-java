package com.lokesh.gateway.service.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String executionId, Object payload) {
        messagingTemplate.convertAndSend("/topic/executions/" + executionId, payload);
    }
}

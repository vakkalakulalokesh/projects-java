package com.lokesh.jobscheduler.service.controller;

import com.lokesh.jobscheduler.common.dto.ExecutionLogEntry;
import com.lokesh.jobscheduler.common.event.ExecutionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendExecutionEvent(String tenantId, ExecutionEvent event) {
        messagingTemplate.convertAndSend("/topic/executions/" + tenantId, event);
    }

    public void sendLogEntry(String executionId, ExecutionLogEntry entry) {
        messagingTemplate.convertAndSend("/topic/logs/" + executionId, entry);
    }
}

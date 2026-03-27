package com.lokesh.jobscheduler.service.rabbitmq;

import com.lokesh.jobscheduler.common.dto.WorkerHeartbeat;
import com.lokesh.jobscheduler.service.service.WorkerRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HeartbeatConsumer {

    private final WorkerRegistryService workerRegistryService;

    @RabbitListener(queues = RabbitMqNames.QUEUE_WORKER_HEARTBEAT)
    public void onHeartbeat(WorkerHeartbeat heartbeat) {
        workerRegistryService.processHeartbeat(heartbeat);
    }
}

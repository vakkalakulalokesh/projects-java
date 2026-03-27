package com.lokesh.jobscheduler.service.rabbitmq;

import com.lokesh.jobscheduler.common.dto.WorkerHeartbeat;
import com.lokesh.jobscheduler.service.service.WorkerActivityTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HeartbeatProducer {

    private final RabbitTemplate rabbitTemplate;
    private final WorkerActivityTracker workerActivityTracker;

    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String workerId = hostname + "-scheduler";
            int active = workerActivityTracker.getActiveExecutions();
            String status = active > 0 ? "BUSY" : "ONLINE";
            WorkerHeartbeat heartbeat = new WorkerHeartbeat(
                    workerId,
                    hostname,
                    active,
                    3,
                    status,
                    LocalDateTime.now(),
                    ManagementFactory.getRuntimeMXBean().getUptime()
            );
            rabbitTemplate.convertAndSend(RabbitMqNames.EXCHANGE_WORKER_HEARTBEAT, RabbitMqNames.RK_WORKER_HEARTBEAT, heartbeat);
        } catch (Exception ignored) {
        }
    }
}

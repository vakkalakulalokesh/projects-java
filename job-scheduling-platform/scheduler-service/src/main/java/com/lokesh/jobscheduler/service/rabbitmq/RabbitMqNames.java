package com.lokesh.jobscheduler.service.rabbitmq;

public final class RabbitMqNames {

    public static final String EXCHANGE_JOB_EXECUTION = "job.execution";
    public static final String EXCHANGE_JOB_EVENTS = "job.events";
    public static final String EXCHANGE_JOB_RETRY = "job.retry";
    public static final String EXCHANGE_JOB_DLX = "job.dlx";

    public static final String QUEUE_JOB_WORK = "job.work.queue";
    public static final String QUEUE_JOB_RETRY = "job.retry.queue";
    public static final String QUEUE_JOB_DLQ = "job.dlq";
    public static final String QUEUE_JOB_EVENTS = "job.events.queue";
    public static final String QUEUE_WORKER_HEARTBEAT = "worker.heartbeat";

    public static final String EXCHANGE_WORKER_HEARTBEAT = "worker.heartbeat.exchange";
    public static final String RK_WORKER_HEARTBEAT = "heartbeat";

    public static final String RK_JOB_DISPATCH = "job.dispatch";
    public static final String RK_JOB_RETRY = "job.retry";
    public static final String RK_JOB_DLQ = "job.dead";

    private RabbitMqNames() {
    }
}

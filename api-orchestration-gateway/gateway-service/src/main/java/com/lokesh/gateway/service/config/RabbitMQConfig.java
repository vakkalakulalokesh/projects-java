package com.lokesh.gateway.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String STEP_EXECUTION_EXCHANGE = "step.execution";
    public static final String STEP_RESULTS_EXCHANGE = "step.results";
    public static final String STEP_RETRY_EXCHANGE = "step.retry";
    public static final String STEP_REDELIVERY_FANOUT = "step.redelivery";
    public static final String GATEWAY_DLX_EXCHANGE = "gateway.dlx";
    public static final String STEP_DLQ = "step.dlq";
    public static final String STEP_RESULT_QUEUE = "step.result.queue";
    public static final String STEP_RETRY_TTL_QUEUE = "step.retry.ttl";
    public static final String STEP_REDELIVERY_QUEUE = "step.redelivery.queue";
    public static final String HEADER_TARGET_ROUTING_KEY = "gw-target-rk";

    public static final String Q_HTTP = "step.http.queue";
    public static final String Q_TRANSFORM = "step.transform.queue";
    public static final String Q_CONDITION = "step.condition.queue";
    public static final String Q_DELAY = "step.delay.queue";
    public static final String Q_AGGREGATE = "step.aggregate.queue";
    public static final String Q_SCRIPT = "step.script.queue";

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setPrefetchCount(10);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        return factory;
    }

    @Bean
    Declarables rabbitTopology() {
        TopicExchange stepExecution = new TopicExchange(STEP_EXECUTION_EXCHANGE, true, false);
        DirectExchange stepResults = new DirectExchange(STEP_RESULTS_EXCHANGE, true, false);
        DirectExchange stepRetry = new DirectExchange(STEP_RETRY_EXCHANGE, true, false);
        FanoutExchange stepRedelivery = new FanoutExchange(STEP_REDELIVERY_FANOUT, true, false);
        FanoutExchange executionEvents = new FanoutExchange("execution.events", true, false);
        DirectExchange gatewayDlx = new DirectExchange(GATEWAY_DLX_EXCHANGE, true, false);

        Queue httpQueue = durableWorkerQueue(Q_HTTP);
        Queue transformQueue = durableWorkerQueue(Q_TRANSFORM);
        Queue conditionQueue = durableWorkerQueue(Q_CONDITION);
        Queue delayQueue = durableWorkerQueue(Q_DELAY);
        Queue aggregateQueue = durableWorkerQueue(Q_AGGREGATE);
        Queue scriptQueue = durableWorkerQueue(Q_SCRIPT);

        Queue resultQueue = QueueBuilder.durable(STEP_RESULT_QUEUE).build();

        Queue retryTtl = QueueBuilder.durable(STEP_RETRY_TTL_QUEUE)
                .ttl(5_000)
                .deadLetterExchange(STEP_REDELIVERY_FANOUT)
                .build();

        Queue redeliveryQueue = QueueBuilder.durable(STEP_REDELIVERY_QUEUE).build();

        Queue dlq = QueueBuilder.durable(STEP_DLQ).build();

        return new Declarables(
                stepExecution,
                stepResults,
                stepRetry,
                stepRedelivery,
                executionEvents,
                gatewayDlx,
                httpQueue,
                transformQueue,
                conditionQueue,
                delayQueue,
                aggregateQueue,
                scriptQueue,
                resultQueue,
                retryTtl,
                redeliveryQueue,
                dlq,
                BindingBuilder.bind(httpQueue).to(stepExecution).with("step.http"),
                BindingBuilder.bind(transformQueue).to(stepExecution).with("step.transform"),
                BindingBuilder.bind(conditionQueue).to(stepExecution).with("step.condition"),
                BindingBuilder.bind(delayQueue).to(stepExecution).with("step.delay"),
                BindingBuilder.bind(aggregateQueue).to(stepExecution).with("step.aggregate"),
                BindingBuilder.bind(scriptQueue).to(stepExecution).with("step.script"),
                BindingBuilder.bind(resultQueue).to(stepResults).with("result"),
                BindingBuilder.bind(retryTtl).to(stepRetry).with("retry"),
                BindingBuilder.bind(redeliveryQueue).to(stepRedelivery),
                BindingBuilder.bind(dlq).to(gatewayDlx).with("dead")
        );
    }

    private static Queue durableWorkerQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", GATEWAY_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead")
                .build();
    }
}

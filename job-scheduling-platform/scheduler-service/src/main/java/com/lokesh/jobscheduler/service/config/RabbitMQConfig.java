package com.lokesh.jobscheduler.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.jobscheduler.service.rabbitmq.RabbitMqNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Bean
    public DirectExchange jobExecutionExchange() {
        return new DirectExchange(RabbitMqNames.EXCHANGE_JOB_EXECUTION, true, false);
    }

    @Bean
    public FanoutExchange jobEventsExchange() {
        return new FanoutExchange(RabbitMqNames.EXCHANGE_JOB_EVENTS, true, false);
    }

    @Bean
    public DirectExchange jobRetryExchange() {
        return new DirectExchange(RabbitMqNames.EXCHANGE_JOB_RETRY, true, false);
    }

    @Bean
    public DirectExchange jobDlxExchange() {
        return new DirectExchange(RabbitMqNames.EXCHANGE_JOB_DLX, true, false);
    }

    @Bean
    public DirectExchange workerHeartbeatExchange() {
        return new DirectExchange(RabbitMqNames.EXCHANGE_WORKER_HEARTBEAT, true, false);
    }

    @Bean
    public Queue jobWorkQueue() {
        return QueueBuilder.durable(RabbitMqNames.QUEUE_JOB_WORK)
                .maxPriority(10)
                .build();
    }

    @Bean
    public Queue jobRetryQueue() {
        return QueueBuilder.durable(RabbitMqNames.QUEUE_JOB_RETRY)
                .withArgument("x-dead-letter-exchange", RabbitMqNames.EXCHANGE_JOB_EXECUTION)
                .withArgument("x-dead-letter-routing-key", RabbitMqNames.RK_JOB_DISPATCH)
                .build();
    }

    @Bean
    public Queue jobDlq() {
        return QueueBuilder.durable(RabbitMqNames.QUEUE_JOB_DLQ).build();
    }

    @Bean
    public Queue jobEventsQueue() {
        return QueueBuilder.durable(RabbitMqNames.QUEUE_JOB_EVENTS).build();
    }

    @Bean
    public Queue workerHeartbeatQueue() {
        return QueueBuilder.durable(RabbitMqNames.QUEUE_WORKER_HEARTBEAT).build();
    }

    @Bean
    public Binding jobWorkBinding() {
        return BindingBuilder.bind(jobWorkQueue())
                .to(jobExecutionExchange())
                .with(RabbitMqNames.RK_JOB_DISPATCH);
    }

    @Bean
    public Binding jobRetryBinding() {
        return BindingBuilder.bind(jobRetryQueue())
                .to(jobRetryExchange())
                .with(RabbitMqNames.RK_JOB_RETRY);
    }

    @Bean
    public Binding jobDlqBinding() {
        return BindingBuilder.bind(jobDlq())
                .to(jobDlxExchange())
                .with(RabbitMqNames.RK_JOB_DLQ);
    }

    @Bean
    public Binding jobEventsFanoutBinding() {
        return BindingBuilder.bind(jobEventsQueue())
                .to(jobEventsExchange());
    }

    @Bean
    public Binding workerHeartbeatBinding() {
        return BindingBuilder.bind(workerHeartbeatQueue())
                .to(workerHeartbeatExchange())
                .with(RabbitMqNames.RK_WORKER_HEARTBEAT);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}

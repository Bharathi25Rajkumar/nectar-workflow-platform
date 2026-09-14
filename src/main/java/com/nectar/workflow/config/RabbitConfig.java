package com.nectar.workflow.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "nectar.exchange";
    public static final String TASK_QUEUE = "nectar.task.queue";
    public static final String TASK_DLQ = "nectar.task.dlq";
    public static final String ROUTING_KEY = "task.event";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(TASK_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "task.dlq")
                .build();
    }

    @Bean
    public Queue taskDlq() {
        return QueueBuilder.durable(TASK_DLQ).build();
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue()).to(exchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(taskDlq()).to(exchange()).with("task.dlq");
    }
}

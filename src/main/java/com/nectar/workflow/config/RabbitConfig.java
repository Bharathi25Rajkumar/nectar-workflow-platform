package com.nectar.workflow.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${nectar.rabbitmq.exchange:nectar.exchange}")
    private String exchangeName;

    @Value("${nectar.rabbitmq.queues.task:nectar.task.queue}")
    private String taskQueueName;

    @Value("${nectar.rabbitmq.queues.task-dlq:nectar.task.dlq}")
    private String taskDlqName;

    @Value("${nectar.rabbitmq.routing-keys.task:task.event}")
    private String taskRoutingKey;

    @Value("${nectar.rabbitmq.routing-keys.task-dlq:task.dlq}")
    private String taskDlqRoutingKey;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(taskQueueName)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", taskDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue taskDlq() {
        return QueueBuilder.durable(taskDlqName).build();
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue()).to(exchange()).with(taskRoutingKey);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(taskDlq()).to(exchange()).with(taskDlqRoutingKey);
    }

}

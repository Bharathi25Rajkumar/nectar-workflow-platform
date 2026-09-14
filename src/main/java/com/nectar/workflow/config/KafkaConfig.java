package com.nectar.workflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic workflowEventsTopic() {
        return TopicBuilder.name("nectar.workflow.events")
                .partitions(12)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskEventsTopic() {
        return TopicBuilder.name("nectar.task.events")
                .partitions(12)
                .replicas(1)
                .build();
    }
}

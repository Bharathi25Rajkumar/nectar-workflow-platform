package com.nectar.workflow.config;

import com.nectar.workflow.messaging.kafka.DomainEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Configuration
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;

    @Value("${nectar.kafka.topics.workflow-events:nectar.workflow.events}")
    private String workflowTopic;

    @Value("${nectar.kafka.topics.task-events:nectar.task.events}")
    private String taskTopic;

    public KafkaConfig(KafkaProperties kafkaProperties, JsonMapper jsonMapper) {
        this.kafkaProperties = kafkaProperties;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public ProducerFactory<String, DomainEvent> producerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        JacksonJsonSerializer<DomainEvent> valueSerializer = new JacksonJsonSerializer<>(jsonMapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, DomainEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic workflowEventsTopic() {
        return TopicBuilder.name(workflowTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic taskEventsTopic() {
        return TopicBuilder.name(taskTopic).partitions(6).replicas(1).build();
    }
}

package com.nectar.workflow.outbox;

import com.nectar.workflow.entity.OutboxEvent;
import com.nectar.workflow.messaging.kafka.DomainEvent;
import com.nectar.workflow.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${nectar.kafka.topics.workflow-events:nectar.workflow.events}")
    private String workflowTopic;

    @Value("${nectar.kafka.topics.task-events:nectar.task.events}")
    private String taskTopic;

    @Value("${nectar.rabbitmq.exchange:nectar.exchange}")
    private String rabbitExchange;

    @Value("${nectar.rabbitmq.routing-keys.task:task.event}")
    private String rabbitRoutingKey;

    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                        @Autowired(required = false) KafkaTemplate<String, DomainEvent> kafkaTemplate,
                        @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void poll() {
        List<OutboxEvent> batch = outboxEventRepository
                .findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100));
        if (batch.isEmpty()) return;

        for (OutboxEvent event : batch) {
            try {
                String key = event.getTenantId().toString();
                if (kafkaTemplate != null) {

                    DomainEvent domainEvent = new DomainEvent(
                            event.getId(),
                            event.getEventType(),
                            event.getAggregateType(),
                            event.getAggregateId(),
                            event.getTenantId(),
                            event.getPayload(),
                            event.getCreatedAt()
                    );

                    String topic = resolveTopic(event.getAggregateType());

                    ProducerRecord<String, DomainEvent> record =
                            new ProducerRecord<>(topic, key, domainEvent);

                    record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
                    try {
                        kafkaTemplate.send(record).get(3, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("Kafka send failed for {}", event.getId(), e);
                        throw new RuntimeException(e);
                    }
                }

                if (rabbitTemplate != null && "TASK".equalsIgnoreCase(event.getAggregateType())) {
                    rabbitTemplate.convertAndSend(rabbitExchange, rabbitRoutingKey, event.getPayload(),
                            m -> {
                                m.getMessageProperties().setHeader("eventId", event.getId().toString());
                                return m;
                            });
                }

                log.info("Publishing outbox event {} type={} tenant={} aggregate={}:{}",
                        event.getId(), event.getEventType(), event.getTenantId(),
                        event.getAggregateType(), event.getAggregateId()
                );

                event.markPublished();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage(), e);
            }
        }
    }

    public String resolveTopic(String type){
        if("WORKFLOW".equalsIgnoreCase(type)){
            return workflowTopic;
        }

        return taskTopic;
    }
}

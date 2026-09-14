package com.nectar.workflow.outbox;

import com.nectar.workflow.entity.OutboxEvent;
import com.nectar.workflow.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                        @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate,
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
                if(kafkaTemplate != null){
                    ProducerRecord<String, String> record =
                            new ProducerRecord<>("nectar.task.events", key, event.getPayload());
                    record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
                    try {
                        kafkaTemplate.send(record).get(3, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("Kafka send failed for {}", event.getId(), e);
                        throw new RuntimeException(e);
                    }
                }

                if (rabbitTemplate != null) {
                    rabbitTemplate.convertAndSend("nectar.exchange", "task.event", event.getPayload(),
                            m -> {
                                m.getMessageProperties().setHeader("eventId", event.getId().toString());
                                return m;
                            });
                }

                log.info("Publishing outbox event {} type={} tenant={} aggregate={}:{}",
                        event.getId(), event.getEventType(), event.getTenantId(),
                        event.getAggregateType(), event.getAggregateId());
                event.markPublished();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage(), e);
            }
        }
    }
}

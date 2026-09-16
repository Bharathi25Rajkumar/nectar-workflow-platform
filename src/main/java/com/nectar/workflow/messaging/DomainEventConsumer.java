package com.nectar.workflow.messaging;


import com.nectar.workflow.entity.ProcessedEvent;
import com.nectar.workflow.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;

    public DomainEventConsumer(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = "${nectar.kafka.topics.task-events:nectar.task.events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleTaskEvent(String payload,
                                @Header(value = "eventId", required = false) String eventIdHeader) {

        handle(payload, eventIdHeader, "task");

    }


    @KafkaListener(topics = "${nectar.kafka.topics.workflow-events:nectar.workflow.events}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleWorkflowEvent(String payload,
                                    @Header(value = "eventId", required = false) String eventIdHeader) {

        handle(payload, eventIdHeader, "workflow");

    }

    private void handle(String payload, String eventIdHeader, String type){
        UUID eventId = resolveEventId(eventIdHeader, payload);

        try {
            processedEventRepository.save(ProcessedEvent.builder().eventId(eventId).build());
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate Kafka {} event {} skipped (idempotency)", type, eventId);
            return;
        }
        log.info("Consumed {} event {} payload={}", type, eventId, payload);
    }

    private UUID resolveEventId(String header, String payload){
        if(header != null){
            try{
                return UUID.fromString(header);
            } catch (Exception ignored) {}
        }

        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
    }
}

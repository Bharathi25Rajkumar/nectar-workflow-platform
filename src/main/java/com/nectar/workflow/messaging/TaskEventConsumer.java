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

import java.util.UUID;

@Component
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);
    private final ProcessedEventRepository processedEventRepository;

    public TaskEventConsumer(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = "nectar.task.events", groupId = "nectar-audit-group")
    @Transactional
    public void handleTaskEvent(String payload,
                                @Header(value = "eventId", required = false) String eventIdHeader) {
        UUID eventId;
        try {
            if (eventIdHeader != null) {
                eventId = UUID.fromString(eventIdHeader);
            } else {
                eventId = UUID.nameUUIDFromBytes(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            eventId = UUID.nameUUIDFromBytes(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try {
            processedEventRepository.save(ProcessedEvent.builder().eventId(eventId).build());
        } catch (
                DataIntegrityViolationException e) {
            log.info("Duplicate Kafka event {} skipped (idempotency)", eventId);
            return;
        }

        log.info("Consumed task event {} payload={}", eventId, payload);

    }

    @KafkaListener(topics = "nectar.workflow.events", groupId = "nectar-audit-group")
    @Transactional
    public void handleWorkflowEvent(String payload,
                                    @Header(value = "eventId", required = false) String eventIdHeader) {
        UUID eventId;
        try {
            if (eventIdHeader != null) {
                eventId = UUID.fromString(eventIdHeader);
            } else {
                eventId = UUID.nameUUIDFromBytes(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            eventId = UUID.nameUUIDFromBytes(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try {
            processedEventRepository.save(ProcessedEvent.builder().eventId(eventId).build());
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate workflow event {} skipped", eventId);
            return;
        }
        log.info("Consumed workflow event {} payload={}", eventId, payload);
    }
}

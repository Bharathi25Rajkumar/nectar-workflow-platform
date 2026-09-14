package com.nectar.workflow.messaging;

import com.nectar.workflow.entity.ProcessedEvent;
import com.nectar.workflow.repository.ProcessedEventRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class TaskRabbitConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskRabbitConsumer.class);
    private final ProcessedEventRepository processedEventRepository;

    public TaskRabbitConsumer(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(queues = "nectar.task.queue", ackMode = "MANUAL")
    @Transactional
    public void handle(String payload,
                       @Header(value = "eventId", required = false) String eventIdHeader,
                       Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
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
            log.info("Duplicate RabbitMQ event {} skipped", eventId);
            channel.basicAck(tag, false);
            return;
        }

        try {
            log.info("Processing RabbitMQ event {} payload={}", eventId, payload);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed RabbitMQ event {}: {}", eventId, e.getMessage(), e);
            channel.basicNack(tag, false, false);
            throw e;
        }
    }
}

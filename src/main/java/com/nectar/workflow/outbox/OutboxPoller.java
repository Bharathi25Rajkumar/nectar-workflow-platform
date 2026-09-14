package com.nectar.workflow.outbox;

import com.nectar.workflow.entity.OutboxEvent;
import com.nectar.workflow.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private final OutboxEventRepository outboxEventRepository;

    public OutboxPoller(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void poll() {
        List<OutboxEvent> batch = outboxEventRepository
                .findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100));
        if (batch.isEmpty()) return;

        for (OutboxEvent event : batch) {
            try {
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

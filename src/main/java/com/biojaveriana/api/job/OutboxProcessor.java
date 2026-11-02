package com.biojaveriana.api.job;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private static final int MAX_ATTEMPTS = 10;
    private static final long GRACE_PERIOD_MINUTES = 5;

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.processing.delayMs:10000}")
    public void processPendingEvents() {
        Instant deadline = Instant.now().minusSeconds(GRACE_PERIOD_MINUTES * 60);

        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEventsOlderThan(
                OutboxEventStatus.PENDING, deadline);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            publishToRabbit(event);
        }

        List<OutboxEvent> failedEvents = outboxRepository.findFailedEventsOlderThan(
                OutboxEventStatus.FAILED, MAX_ATTEMPTS, deadline);

        for (OutboxEvent event : failedEvents) {
            log.info("Retrying previously failed outbox event id={}", event.getId());
            publishToRabbit(event);
        }
    }

    @Transactional
    void publishToRabbit(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.FAILED);
        event.setAttempts(event.getAttempts() + 1);
        event.setUpdatedAt(Instant.now());

        try {
            switch (event.getEventType()) {
                case JOB_CREATED -> {
                    String routingKey = validateQueueKey(event.getQueueKey(), "default");
                    Map<String, Object> message = java.util.Map.of("id", event.getAggregateId());
                    rabbitTemplate.convertAndSend(routingKey, message);
                    log.info("Outbox event {} published JOB_CREATED for job {} to RabbitMQ routing key '{}'",
                            event.getId(), event.getAggregateId(), routingKey);
                }

                case JOB_REPUSHED -> {
                    String routingKey = validateQueueKey(event.getQueueKey(), "default");
                    Map<String, Object> message = java.util.Map.of(
                            "id", event.getAggregateId(),
                            "event_type", "JOB_REPUSHED");
                    rabbitTemplate.convertAndSend(routingKey, message);
                    log.info("Outbox event {} published JOB_REPUSHED for job {} to RabbitMQ routing key '{}'",
                            event.getId(), event.getAggregateId(), routingKey);
                }
            }

            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setUpdatedAt(Instant.now());
        } catch (Exception e) {
            log.error("Failed to publish outbox event {} to RabbitMQ: {}",
                    event.getId(), e.getMessage());
        } finally {
            outboxRepository.save(event);
        }
    }

    private String validateQueueKey(String queueKey, String fallback) {
        if (queueKey == null || queueKey.isBlank()) {
            return fallback;
        }
        return queueKey;
    }
}

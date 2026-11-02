package com.biojaveriana.api.job;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = ?1 AND e.updatedAt < ?2")
    List<OutboxEvent> findPendingEventsOlderThan(OutboxEventStatus status, Instant deadline);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = ?1 AND e.attempts < ?2 AND e.updatedAt < ?3")
    List<OutboxEvent> findFailedEventsOlderThan(OutboxEventStatus status, int maxAttempts, Instant deadline);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = ?1 AND e.queueKey = ?2")
    List<OutboxEvent> findByStatusAndQueueKey(OutboxEventStatus status, String queueKey);
}

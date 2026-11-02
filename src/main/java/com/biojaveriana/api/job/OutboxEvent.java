package com.biojaveriana.api.job;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @Column(name = "payload", nullable = true)
    private String payload;

    @Column(name = "queue_key", nullable = false)
    private String queueKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Default
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Default
    private int attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Default
    private Instant updatedAt = Instant.now();
}

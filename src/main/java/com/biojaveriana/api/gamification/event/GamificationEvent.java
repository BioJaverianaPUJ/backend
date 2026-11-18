package com.biojaveriana.api.gamification.event;

import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Base event for gamification-related domain events.
 * Includes a unique ID for event deduplication to prevent duplicate processing.
 */
public abstract class GamificationEvent extends ApplicationEvent {

    private final UUID id;
    private final UUID userId;

    public GamificationEvent(Object source, UUID userId) {
        super(source);
        this.id = UUID.randomUUID();
        this.userId = userId;
    }

    /**
     * Returns a unique identifier for this event, used for deduplication.
     */
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    /**
     * Returns the event type string for matching with achievement definitions.
     */
    public abstract String getEventType();

    /**
     * Returns additional context data for predicate evaluation.
     */
    public java.util.Map<String, Object> getAdditionalContext() {
        return new java.util.HashMap<>();
    }
}

package com.biojaveriana.api.gamification.event;

import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Base event for gamification-related domain events.
 */
public abstract class GamificationEvent extends ApplicationEvent {

    private final UUID userId;

    public GamificationEvent(Object source, UUID userId) {
        super(source);
        this.userId = userId;
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

package com.biojaveriana.api.gamification.event;

import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Base event for gamification-related domain events.
 * A unique id is attached to each event for potential deduplication.
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
     * Returns a unique identifier for this event.
     */
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    /**
     * Returns the type of this event, used to match against achievement metrics.
     */
    public abstract GamificationEventType getType();
}
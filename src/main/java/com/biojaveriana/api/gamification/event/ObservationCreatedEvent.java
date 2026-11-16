package com.biojaveriana.api.gamification.event;

import java.util.UUID;

/**
 * Event fired when a user creates an observation.
 */
public class ObservationCreatedEvent extends GamificationEvent {

    private final UUID observationId;

    public ObservationCreatedEvent(Object source, UUID userId, UUID observationId) {
        super(source, userId);
        this.observationId = observationId;
    }

    public UUID getObservationId() {
        return observationId;
    }

    @Override
    public String getEventType() {
        return "OBSERVATION_CREATED";
    }
}

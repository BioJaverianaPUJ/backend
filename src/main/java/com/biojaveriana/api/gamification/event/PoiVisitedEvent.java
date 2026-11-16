package com.biojaveriana.api.gamification.event;

import java.util.UUID;

/**
 * Event fired when a user visits a POI.
 */
public class PoiVisitedEvent extends GamificationEvent {

    private final UUID poiId;

    public PoiVisitedEvent(Object source, UUID userId, UUID poiId) {
        super(source, userId);
        this.poiId = poiId;
    }

    public UUID getPoiId() {
        return poiId;
    }

    @Override
    public String getEventType() {
        return "POI_VISITED";
    }
}

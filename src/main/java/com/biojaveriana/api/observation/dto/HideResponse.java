package com.biojaveriana.api.observation.dto;

import java.time.Instant;
import java.util.UUID;

public record HideResponse(
    UUID observationId,
    boolean hidden,
    Instant updatedAt
) {
    public static HideResponse of(UUID observationId, boolean hidden) {
        return new HideResponse(observationId, hidden, Instant.now());
    }
}

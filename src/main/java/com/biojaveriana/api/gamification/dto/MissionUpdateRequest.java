package com.biojaveriana.api.gamification.dto;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

import com.biojaveriana.api.gamification.model.MissionType;

/**
 * Request to update a mission. Only non-null fields are applied; the type,
 * window and predicate are re-validated after the merge.
 */
public record MissionUpdateRequest(
    String name,
    String description,
    Integer xpReward,
    MissionType type,
    Instant startTime,
    Instant endTime,
    JsonNode predicate,
    Boolean active
) {
}

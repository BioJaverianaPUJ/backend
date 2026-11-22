package com.biojaveriana.api.gamification.dto;

import tools.jackson.databind.JsonNode;

/**
 * Request to update an achievement. Only non-null fields are applied.
 */
public record AchievementUpdateRequest(
    String name,
    String description,
    Integer xpReward,
    JsonNode predicate,
    Boolean active
) {
}
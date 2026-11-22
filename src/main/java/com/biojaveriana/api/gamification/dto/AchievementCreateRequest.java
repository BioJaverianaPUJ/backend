package com.biojaveriana.api.gamification.dto;

import tools.jackson.databind.JsonNode;

/**
 * Request to create an achievement. The {@code predicate} is a structured
 * logic-block expression (see PredicateNode).
 */
public record AchievementCreateRequest(
    String slug,
    String name,
    String description,
    Integer xpReward,
    JsonNode predicate
) {
}
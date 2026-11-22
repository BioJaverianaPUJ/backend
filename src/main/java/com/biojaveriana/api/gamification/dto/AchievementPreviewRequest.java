package com.biojaveriana.api.gamification.dto;

import java.util.UUID;

import tools.jackson.databind.JsonNode;

/**
 * Admin dry-run request: evaluate a candidate predicate for a user.
 */
public record AchievementPreviewRequest(
    UUID userId,
    JsonNode predicate
) {
}
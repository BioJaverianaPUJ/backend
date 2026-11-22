package com.biojaveriana.api.gamification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A single achievement shown in a user's progression view: live progress
 * (computed from current metrics) plus earned status.
 */
public record AchievementProgressResponse(
    UUID id,
    String slug,
    String name,
    String description,
    Integer xpReward,
    boolean completed,
    Instant completedAt,
    Long xpAwarded,
    PredicateProgress progress
) {}
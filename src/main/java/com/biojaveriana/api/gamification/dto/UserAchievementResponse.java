package com.biojaveriana.api.gamification.dto;

import java.time.Instant;
import java.util.UUID;

public record UserAchievementResponse(
    UUID id,
    String slug,
    String name,
    String description,
    Integer xpReward,
    Boolean completed,
    Instant completedAt,
    Long xpAwarded
) {
}
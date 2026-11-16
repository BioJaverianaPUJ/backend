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
    Long xpAwarded,
    Integer progress,
    Integer target
) {
    public static UserAchievementResponse from(
        UUID achievementId,
        String slug,
        String name,
        String description,
        Integer xpReward,
        Boolean completed,
        Instant completedAt,
        Long xpAwarded,
        Integer progress,
        Integer target
    ) {
        return new UserAchievementResponse(
            achievementId, slug, name, description, xpReward,
            completed, completedAt, xpAwarded, progress, target
        );
    }
}

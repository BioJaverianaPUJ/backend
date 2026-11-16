package com.biojaveriana.api.gamification.dto;

import java.time.Instant;
import java.util.UUID;

public record AchievementResponse(
    UUID id,
    String slug,
    String name,
    String description,
    Integer xpReward,
    String eventType,
    String predicateJson,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static AchievementResponse from(com.biojaveriana.api.gamification.model.Achievement achievement) {
        return new AchievementResponse(
            achievement.getId(),
            achievement.getSlug(),
            achievement.getName(),
            achievement.getDescription(),
            achievement.getXpReward(),
            achievement.getEventType(),
            achievement.getPredicateJson(),
            achievement.getActive(),
            achievement.getCreatedAt(),
            achievement.getUpdatedAt()
        );
    }
}

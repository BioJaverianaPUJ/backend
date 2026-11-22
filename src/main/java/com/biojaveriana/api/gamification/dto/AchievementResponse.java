package com.biojaveriana.api.gamification.dto;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.biojaveriana.api.gamification.model.Achievement;

public record AchievementResponse(
    UUID id,
    String slug,
    String name,
    String description,
    Integer xpReward,
    JsonNode predicate,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static AchievementResponse from(Achievement achievement, JsonMapper jsonMapper) {
        JsonNode predicate = null;
        if (achievement.getPredicateJson() != null) {
            try {
                predicate = jsonMapper.readTree(achievement.getPredicateJson());
            } catch (Exception ignored) {
                predicate = null;
            }
        }
        return new AchievementResponse(
            achievement.getId(),
            achievement.getSlug(),
            achievement.getName(),
            achievement.getDescription(),
            achievement.getXpReward(),
            predicate,
            achievement.getActive(),
            achievement.getCreatedAt(),
            achievement.getUpdatedAt()
        );
    }
}
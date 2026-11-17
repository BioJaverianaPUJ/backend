package com.biojaveriana.api.gamification.dto;

public record AchievementCreateRequest(
    String slug,
    String name,
    String description,
    Integer xpReward,
    String eventType,
    String predicateJson
) {
}

package com.biojaveriana.api.gamification.dto;

import java.util.UUID;

public record AchievementCreateRequest(
    String slug,
    String name,
    String description,
    Integer xpReward,
    String eventType,
    String predicateJson
) {
}

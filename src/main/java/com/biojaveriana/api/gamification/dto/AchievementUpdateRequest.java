package com.biojaveriana.api.gamification.dto;

import java.util.UUID;

public record AchievementUpdateRequest(
    String name,
    String description,
    Integer xpReward,
    String eventType,
    String predicateJson,
    Boolean active
) {
}

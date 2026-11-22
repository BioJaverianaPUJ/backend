package com.biojaveriana.api.gamification.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;

/**
 * A single mission in a user's mission board or history: the mission
 * template's details, the user's period/status, and (for board entries)
 * live progress toward the goal.
 */
public record UserMissionResponse(
    UUID id,
    String slug,
    String name,
    String description,
    Integer xpReward,
    MissionType type,
    Instant periodStart,
    Instant periodEnd,
    MissionStatus status,
    Instant completedAt,
    Long xpAwarded,
    PredicateProgress progress
) {
}

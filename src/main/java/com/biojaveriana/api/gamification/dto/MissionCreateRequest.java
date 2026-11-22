package com.biojaveriana.api.gamification.dto;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

import com.biojaveriana.api.gamification.model.MissionType;

/**
 * Request to create a mission. The {@code predicate} is a structured
 * logic-block expression (see PredicateNode) over windowed metrics.
 * {@code startTime}/{@code endTime} are required for EVENT missions and must
 * be omitted for DAILY/WEEKLY missions (their window comes from the
 * assignment).
 */
public record MissionCreateRequest(
    String slug,
    String name,
    String description,
    Integer xpReward,
    MissionType type,
    Instant startTime,
    Instant endTime,
    JsonNode predicate
) {
}

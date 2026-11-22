package com.biojaveriana.api.gamification.dto;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionType;

/**
 * Response representing a mission template.
 */
public record MissionResponse(
    UUID id,
    String slug,
    String name,
    String description,
    Integer xpReward,
    MissionType type,
    Instant startTime,
    Instant endTime,
    JsonNode predicate,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static MissionResponse from(Mission mission, JsonMapper jsonMapper) {
        JsonNode predicate = null;
        if (mission.getPredicateJson() != null) {
            try {
                predicate = jsonMapper.readTree(mission.getPredicateJson());
            } catch (Exception ignored) {
                predicate = null;
            }
        }
        return new MissionResponse(
            mission.getId(),
            mission.getSlug(),
            mission.getName(),
            mission.getDescription(),
            mission.getXpReward(),
            mission.getType(),
            mission.getStartTime(),
            mission.getEndTime(),
            predicate,
            mission.getActive(),
            mission.getCreatedAt(),
            mission.getUpdatedAt()
        );
    }
}

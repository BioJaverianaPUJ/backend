package com.biojaveriana.api.gamification.dto;

import java.util.List;

/**
 * The user's current mission board, grouped by mission type. The {@code event}
 * list also includes in-window events the user has already completed.
 */
public record MyMissionsResponse(
    List<UserMissionResponse> daily,
    List<UserMissionResponse> weekly,
    List<UserMissionResponse> event
) {
}

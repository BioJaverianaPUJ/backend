package com.biojaveriana.api.gamification.dto;

import java.util.UUID;

public record EvaluateResponse(
    UUID achievementId,
    String slug,
    Boolean satisfied,
    String message
) {
}

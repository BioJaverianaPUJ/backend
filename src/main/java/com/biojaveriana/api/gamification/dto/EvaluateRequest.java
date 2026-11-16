package com.biojaveriana.api.gamification.dto;

import java.util.UUID;
import java.util.Map;

public record EvaluateRequest(
    UUID userId,
    Map<String, Object> context
) {
}

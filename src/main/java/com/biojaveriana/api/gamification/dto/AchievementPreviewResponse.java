package com.biojaveriana.api.gamification.dto;

import java.util.Map;

/**
 * Result of a predicate dry-run.
 */
public record AchievementPreviewResponse(
    Boolean satisfied,
    Map<String, Long> metricValues
) {
}
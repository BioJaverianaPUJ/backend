package com.biojaveriana.api.gamification.dto;

import java.util.UUID;

public record UserGamificationResponse(
    UUID userId,
    String username,
    Long totalXp,
    Integer level,
    Long xpForCurrentLevel,
    Long xpForNextLevel,
    Double progressPercent
) {
    public static UserGamificationResponse from(
        Long totalXp,
        Integer level,
        Long xpForCurrentLevel,
        Long xpForNextLevel
    ) {
        double progressPercent = xpForNextLevel > xpForCurrentLevel
            ? ((totalXp - xpForCurrentLevel) / (double) (xpForNextLevel - xpForCurrentLevel)) * 100.0
            : 100.0;
        progressPercent = Math.min(100.0, Math.max(0.0, progressPercent));
        return new UserGamificationResponse(
            null, null, totalXp, level, xpForCurrentLevel, xpForNextLevel, progressPercent
        );
    }

    public static UserGamificationResponse from(
        UUID userId,
        String username,
        Long totalXp,
        Integer level,
        Long xpForCurrentLevel,
        Long xpForNextLevel
    ) {
        return new UserGamificationResponse(
            userId, username, totalXp, level, xpForCurrentLevel, xpForNextLevel, null
        );
    }
}

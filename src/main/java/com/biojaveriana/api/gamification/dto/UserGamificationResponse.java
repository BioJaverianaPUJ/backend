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
        Long totalXp, Integer level, Long xpForCurrentLevel, Long xpForNextLevel
    ) {
        return new UserGamificationResponse(null, null, totalXp, level, xpForCurrentLevel, xpForNextLevel,
            progress(totalXp, xpForCurrentLevel, xpForNextLevel));
    }

    public static UserGamificationResponse from(
        UUID userId, String username, Long totalXp, Integer level, Long xpForCurrentLevel, Long xpForNextLevel
    ) {
        return new UserGamificationResponse(userId, username, totalXp, level, xpForCurrentLevel, xpForNextLevel,
            progress(totalXp, xpForCurrentLevel, xpForNextLevel));
    }

    private static double progress(Long totalXp, Long xpForCurrentLevel, Long xpForNextLevel) {
        if (xpForNextLevel == null || xpForCurrentLevel == null || xpForNextLevel <= xpForCurrentLevel) {
            return 100.0;
        }
        double percent = ((totalXp - xpForCurrentLevel) / (double) (xpForNextLevel - xpForCurrentLevel)) * 100.0;
        return Math.min(100.0, Math.max(0.0, percent));
    }
}
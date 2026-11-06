package com.biojaveriana.api.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.user.Ban;
import com.biojaveriana.api.user.BanStatus;
import com.biojaveriana.api.user.BanType;

public record BanResponse(
    UUID id,
    UUID userId,
    String username,
    String reason,
    BanType banType,
    BanStatus status,
    Instant expiresAt,
    Instant createdAt
) {
    public static BanResponse from(Ban ban) {
        return new BanResponse(
            ban.getId(),
            ban.getUser().getId(),
            ban.getUser().getUsername(),
            ban.getReason(),
            ban.getBanType(),
            ban.getStatus(),
            ban.getExpiresAt(),
            ban.getCreatedAt()
        );
    }
}
package com.biojaveriana.api.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.user.Ban;
import com.biojaveriana.api.user.BanStatus;
import com.biojaveriana.api.user.BanType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body containing ban details", example = "{\"id\": \"660e8400-e29b-41d4-a716-446655440001\", \"userId\": \"550e8400-e29b-41d4-a716-446655440000\", \"username\": \"john_doe\", \"reason\": \"Repeated violations of community guidelines\", \"banType\": \"TEMPORAL\", \"status\": \"ACTIVE\", \"expiresAt\": \"2026-12-31T23:59:59Z\", \"createdAt\": \"2026-06-15T10:00:00Z\"}")
public record BanResponse(
    @Schema(description = "Unique ban identifier", example = "660e8400-e29b-41d4-a716-446655440001")
    UUID id,

    @Schema(description = "ID of the banned user", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID userId,

    @Schema(description = "Username of the banned user", example = "john_doe")
    String username,

    @Schema(description = "Reason for the ban", example = "Repeated violations of community guidelines")
    String reason,

    @Schema(description = "Type of ban", example = "TEMPORAL", allowableValues = {"TEMPORAL", "PERMANENT"})
    BanType banType,

    @Schema(description = "Current status of the ban", example = "ACTIVE", allowableValues = {"ACTIVE", "EXPIRED", "CANCELLED"})
    BanStatus status,

    @Schema(description = "Expiration date for temporary bans", example = "2026-12-31T23:59:59Z")
    Instant expiresAt,

    @Schema(description = "Timestamp when the ban was created", example = "2026-06-15T10:00:00Z")
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
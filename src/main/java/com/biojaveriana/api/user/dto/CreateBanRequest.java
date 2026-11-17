package com.biojaveriana.api.user.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for creating a user ban", example = "{\"userId\": \"550e8400-e29b-41d4-a716-446655440000\", \"reason\": \"Repeated violations of community guidelines\", \"banType\": \"TEMPORAL\", \"expiresAt\": \"2026-12-31T23:59:59Z\"}")
public record CreateBanRequest(
    @Schema(description = "ID of the user to ban", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull
    UUID userId,

    @Schema(description = "Reason for the ban", example = "Repeated violations of community guidelines")
    @NotBlank
    String reason,

    @Schema(description = "Type of ban", example = "TEMPORAL", allowableValues = {"TEMPORAL", "PERMANENT"})
    @NotNull
    BanTypeRequest banType,

    @Schema(description = "Expiration date for temporary bans (ignored for PERMANENT)", example = "2026-12-31T23:59:59Z")
    Instant expiresAt
) {
    public enum BanTypeRequest {
        TEMPORAL,
        PERMANENT
    }
}
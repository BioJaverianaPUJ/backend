package com.biojaveriana.api.user.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBanRequest(
    @NotNull
    UUID userId,

    @NotBlank
    String reason,

    @NotNull
    BanTypeRequest banType,

    Instant expiresAt
) {
    public enum BanTypeRequest {
        TEMPORAL,
        PERMANENT
    }
}
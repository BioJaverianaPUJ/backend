package com.biojaveriana.api.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The current status of a ban", example = "ACTIVE")
public enum BanStatus {
    @Schema(description = "Ban is currently active")
    ACTIVE,
    @Schema(description = "Ban has expired")
    EXPIRED,
    @Schema(description = "Ban was cancelled by an admin")
    CANCELLED
}
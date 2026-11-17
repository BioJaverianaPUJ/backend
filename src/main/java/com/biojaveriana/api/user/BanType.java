package com.biojaveriana.api.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The type of ban imposed on a user", example = "TEMPORAL")
public enum BanType {
    @Schema(description = "Temporary ban with an expiration date")
    TEMPORAL,
    @Schema(description = "Permanent ban with no expiration")
    PERMANENT
}
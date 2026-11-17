package com.biojaveriana.api.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The user's role in the system", example = "USER")
public enum Role {
    @Schema(description = "Regular user with standard permissions")
    USER,
    @Schema(description = "Administrator with moderation capabilities")
    ADMIN,
    @Schema(description = "Super administrator with full system access")
    SUPERADMIN
}

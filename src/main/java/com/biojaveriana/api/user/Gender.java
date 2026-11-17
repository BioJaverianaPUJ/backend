package com.biojaveriana.api.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The user's biological sex category", example = "MALE")
public enum Gender {
    @Schema(description = "Male")
    MALE,
    @Schema(description = "Female")
    FEMALE,
    @Schema(description = "Non-binary")
    NON_BINARY,
    @Schema(description = "Other")
    OTHER
}

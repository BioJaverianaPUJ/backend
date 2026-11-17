package com.biojaveriana.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for user login", example = "{\"email\": \"user@example.com\", \"password\": \"MySecurePass123!\"}")
public record LoginRequest(
    @Schema(description = "User email address", example = "user@example.com")
    @NotBlank
    @Email
    String email,

    @Schema(description = "User password (min 8 characters)", example = "MySecurePass123!")
    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {}

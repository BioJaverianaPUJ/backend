package com.biojaveriana.api.auth.dto;

import com.biojaveriana.api.user.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for user registration", example = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"username\": \"john_doe\", \"email\": \"user@example.com\", \"gender\": \"MALE\", \"password\": \"MySecurePass123!\"}")
public record RegistrationRequest(
    @Schema(description = "User's first name", example = "John")
    @NotBlank
    String firstName,

    @Schema(description = "User's last name", example = "Doe")
    @NotBlank
    String lastName,

    @Schema(description = "Desired username (unique)", example = "john_doe")
    @NotBlank
    @Size(min = 3, max = 50)
    String username,

    @Schema(description = "User email address", example = "user@example.com")
    @NotBlank
    @Email
    String email,

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "NON_BINARY", "OTHER"})
    @NotNull
    Gender gender,

    @Schema(description = "User password (min 8 characters)", example = "MySecurePass123!")
    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {}

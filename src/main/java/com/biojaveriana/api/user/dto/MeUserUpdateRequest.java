package com.biojaveriana.api.user.dto;

import com.biojaveriana.api.user.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating the current user's own profile", example = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"username\": \"john_doe\", \"gender\": \"MALE\"}")
public record MeUserUpdateRequest(
    @Schema(description = "User's first name", example = "John")
    @Size(min = 1, max = 50)
    String firstName,

    @Schema(description = "User's last name", example = "Doe")
    @Size(min = 1, max = 50)
    String lastName,

    @Schema(description = "User's username", example = "john_doe")
    @Size(min = 3, max = 25)
    String username,

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "NON_BINARY", "OTHER"})
    Gender gender
) {}

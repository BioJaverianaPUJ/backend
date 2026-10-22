package com.biojaveriana.api.auth.dto;

import com.biojaveriana.api.user.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
    @NotBlank
    String firstName,

    @NotBlank
    String lastName,

    @NotBlank
    @Size(min = 3, max = 50)
    String username,

    @NotBlank
    @Email
    String email,

    @NotNull
    Gender gender,

    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {}

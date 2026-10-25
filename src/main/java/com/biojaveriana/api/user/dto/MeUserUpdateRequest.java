package com.biojaveriana.api.user.dto;

import com.biojaveriana.api.user.Gender;
import jakarta.validation.constraints.Size;

public record MeUserUpdateRequest(
    @Size(min = 1, max = 50) String firstName,
    @Size(min = 1, max = 50) String lastName,
    @Size(min = 3, max = 25) String username,
    Gender gender,
    String profilePictureKey,
    String bannerKey
) {}

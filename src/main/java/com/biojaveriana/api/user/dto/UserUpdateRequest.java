package com.biojaveriana.api.user.dto;

import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(min = 1, max = 50) String firstName,
    @Size(min = 1, max = 50) String lastName,
    @Size(min = 3, max = 25) String username,
    Role role,
    Gender gender,
    Boolean enabled,
    String profilePictureKey,
    String bannerKey
) {}

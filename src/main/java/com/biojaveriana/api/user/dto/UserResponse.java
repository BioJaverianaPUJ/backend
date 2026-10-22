package com.biojaveriana.api.user.dto;

import java.util.UUID;

import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

public record UserResponse(
    UUID id,
    String firstName,
    String lastName,
    String username,
    String email,
    Role role,
    Gender gender
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getGender()
        );
    }
}

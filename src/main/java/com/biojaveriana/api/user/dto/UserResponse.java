package com.biojaveriana.api.user.dto;

import java.util.UUID;

import com.biojaveriana.api.s3.S3UrlGenerator;
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
    Gender gender,
    String profilePictureUrl,
    String bannerUrl
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getGender(),
            null,
            null
        );
    }

    public static UserResponse from(User user, S3UrlGenerator generator) {
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getGender(),
            generator.generateUrl(user.getProfilePictureKey()),
            generator.generateUrl(user.getBannerKey())
        );
    }
}

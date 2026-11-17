package com.biojaveriana.api.user.dto;

import java.util.UUID;

import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body containing user profile information", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"username\": \"john_doe\", \"email\": \"john@example.com\", \"role\": \"USER\", \"gender\": \"MALE\", \"profilePictureUrl\": null, \"bannerUrl\": null}")
public record UserResponse(
    @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "User's first name", example = "John")
    String firstName,

    @Schema(description = "User's last name", example = "Doe")
    String lastName,

    @Schema(description = "User's username", example = "john_doe")
    String username,

    @Schema(description = "User's email address", example = "john@example.com")
    String email,

    @Schema(description = "User's role", example = "USER", allowableValues = {"USER", "ADMIN", "SUPERADMIN"})
    Role role,

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "NON_BINARY", "OTHER"})
    Gender gender,

    @Schema(description = "URL to the user's profile picture", example = "https://s3.example.com/uploads/profile/abc123.jpg")
    String profilePictureUrl,

    @Schema(description = "URL to the user's banner image", example = "https://s3.example.com/uploads/banner/def456.jpg")
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

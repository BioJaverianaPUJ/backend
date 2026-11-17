package com.biojaveriana.api.comment.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact user summary for comment/post authors", example = "{\"id\": \"660e8400-e29b-41d4-a716-446655440001\", \"username\": \"john_doe\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"profilePictureUrl\": null}")
public record UserSummaryResponse(
    @Schema(description = "Unique user identifier", example = "660e8400-e29b-41d4-a716-446655440001")
    UUID id,

    @Schema(description = "User's username", example = "john_doe")
    String username,

    @Schema(description = "User's first name", example = "John")
    String firstName,

    @Schema(description = "User's last name", example = "Doe")
    String lastName,

    @Schema(description = "URL to the user's profile picture", example = "https://s3.example.com/uploads/profile/abc123.jpg")
    String profilePictureUrl
) {
}

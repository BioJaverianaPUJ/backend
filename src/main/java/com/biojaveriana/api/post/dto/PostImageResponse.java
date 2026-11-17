package com.biojaveriana.api.post.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Post image metadata", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"s3Key\": \"uploads/post/abc123.jpg\", \"url\": \"https://s3.example.com/uploads/post/abc123.jpg\", \"orderIndex\": 0}")
public record PostImageResponse(
    @Schema(description = "Unique image identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "S3 object key", example = "uploads/post/abc123.jpg")
    String s3Key,

    @Schema(description = "Public URL to the image", example = "https://s3.example.com/uploads/post/abc123.jpg")
    String url,

    @Schema(description = "Display order index", example = "0")
    Integer orderIndex
) {
}

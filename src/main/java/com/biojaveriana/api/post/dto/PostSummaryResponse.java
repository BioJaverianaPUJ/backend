package com.biojaveriana.api.post.dto;

import com.biojaveriana.api.post.Post;
import com.biojaveriana.api.s3.S3UrlGenerator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact post summary for list views", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"author\": {\"id\": \"660e8400-e29b-41d4-a716-446655440001\", \"username\": \"john_doe\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"profilePictureUrl\": null}, \"title\": \"New Species Found\", \"firstImageUrl\": \"https://s3.example.com/uploads/post/abc123.jpg\", \"totalLikes\": 10, \"totalDislikes\": 0, \"banned\": false, \"createdAt\": \"2026-06-15T10:00:00Z\"}")
public record PostSummaryResponse(
    @Schema(description = "Unique post identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Author of the post")
    UserSummaryResponse author,

    @Schema(description = "Title of the post", example = "New Species Found")
    String title,

    @Schema(description = "URL to the first image in the post", example = "https://s3.example.com/uploads/post/abc123.jpg")
    String firstImageUrl,

    @Schema(description = "Total number of likes", example = "10")
    Integer totalLikes,

    @Schema(description = "Total number of dislikes", example = "0")
    Integer totalDislikes,

    @Schema(description = "Whether this post is banned", example = "false")
    Boolean banned,

    @Schema(description = "Timestamp when the post was created", example = "2026-06-15T10:00:00Z")
    Instant createdAt
) {
    public static PostSummaryResponse from(Post post, S3UrlGenerator urlGenerator) {
        String firstImageUrl = null;
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            firstImageUrl = urlGenerator.generateUrl(post.getImages().get(0).getS3Key());
        }

        return new PostSummaryResponse(
            post.getId(),
            new UserSummaryResponse(
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getAuthor().getFirstName(),
                post.getAuthor().getLastName(),
                post.getAuthor().getProfilePictureKey() != null
                    ? urlGenerator.generateUrl(post.getAuthor().getProfilePictureKey())
                    : null),
            post.getTitle(),
            firstImageUrl,
            post.getTotalLikes(),
            post.getTotalDislikes(),
            post.getBanned(),
            post.getCreatedAt()
        );
    }
}

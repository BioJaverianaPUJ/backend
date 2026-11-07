package com.biojaveriana.api.post.dto;

import com.biojaveriana.api.post.Post;
import com.biojaveriana.api.s3.S3UrlGenerator;

import java.time.Instant;
import java.util.UUID;

public record PostSummaryResponse(
    UUID id,
    UserSummaryResponse author,
    String title,
    String firstImageUrl,
    Integer totalLikes,
    Integer totalDislikes,
    Boolean banned,
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

package com.biojaveriana.api.post.dto;

import com.biojaveriana.api.post.VoteType;
import com.biojaveriana.api.post.Post;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostResponse(
    UUID id,
    UserSummaryResponse author,
    String title,
    String description,
    String observation,
    List<PostImageResponse> images,
    Integer totalLikes,
    Integer totalDislikes,
    Boolean userVoted,
    VoteType userVoteType,
    Boolean banned,
    Boolean hidden,
    Instant createdAt,
    Instant updatedAt
) {
    public static PostResponse from(Post post, S3UrlGenerator urlGenerator, User voter) {
        List<PostImageResponse> images = post.getImages() != null
            ? post.getImages().stream()
                .map(img -> new PostImageResponse(
                    img.getId(),
                    img.getS3Key(),
                    urlGenerator.generateUrl(img.getS3Key()),
                    img.getOrderIndex()))
                .toList()
            : List.of();

        Boolean userVoted = null;
        VoteType userVoteType = null;
        if (voter != null) {
            // Will be set by the service layer
        }

        return new PostResponse(
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
            post.getDescription(),
            post.getObservation(),
            images,
            post.getTotalLikes(),
            post.getTotalDislikes(),
            userVoted,
            userVoteType,
            post.getBanned(),
            post.getHidden(),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}

package com.biojaveriana.api.motd.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.biojaveriana.api.motd.FunFact;
import com.biojaveriana.api.s3.S3UrlGenerator;

public record FunFactResponse(
    UUID id,
    UserSummaryResponse author,
    String title,
    String description,
    PoiSummaryResponse poi,
    List<FunFactImageResponse> images,
    Boolean hidden,
    Instant createdAt,
    Instant updatedAt
) {
    public static FunFactResponse from(FunFact funFact, S3UrlGenerator urlGenerator) {
        List<FunFactImageResponse> images = funFact.getImages() != null
            ? funFact.getImages().stream()
                .sorted(Comparator.comparingInt(image -> image.getOrderIndex()))
                .map(img -> new FunFactImageResponse(
                    img.getId(),
                    img.getS3Key(),
                    urlGenerator.generateUrl(img.getS3Key()),
                    img.getOrderIndex()))
                .toList()
            : List.of();

        return new FunFactResponse(
            funFact.getId(),
            new UserSummaryResponse(
                funFact.getAuthor().getId(),
                funFact.getAuthor().getUsername(),
                funFact.getAuthor().getFirstName(),
                funFact.getAuthor().getLastName(),
                funFact.getAuthor().getProfilePictureKey() != null
                    ? urlGenerator.generateUrl(funFact.getAuthor().getProfilePictureKey())
                    : null),
            funFact.getTitle(),
            funFact.getDescription(),
            funFact.getPoi() != null ? PoiSummaryResponse.from(funFact.getPoi()) : null,
            images,
            funFact.getHidden(),
            funFact.getCreatedAt(),
            funFact.getUpdatedAt()
        );
    }
}

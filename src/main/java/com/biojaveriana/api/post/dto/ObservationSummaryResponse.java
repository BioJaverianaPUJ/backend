package com.biojaveriana.api.post.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationType;
import com.biojaveriana.api.s3.S3UrlGenerator;

public record ObservationSummaryResponse(
    UUID id,
    String imageUrl,
    String speciesCommonName,
    ObservationType observationType,
    Boolean checked,
    Double similarityScore,
    Double certainty,
    Instant createdAt
) {
    public static ObservationSummaryResponse from(Observation observation, S3UrlGenerator urlGenerator) {
        return new ObservationSummaryResponse(
            observation.getId(),
            urlGenerator.generateUrl(observation.getReferencePictureKey()),
            observation.getSpeciesCommonName(),
            observation.getObservationType(),
            observation.isChecked(),
            observation.getSimilarityScore(),
            observation.getCertainty(),
            observation.getCreatedAt()
        );
    }
}
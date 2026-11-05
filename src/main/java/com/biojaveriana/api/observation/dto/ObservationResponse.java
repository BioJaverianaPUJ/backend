package com.biojaveriana.api.observation.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationType;
import com.biojaveriana.api.s3.S3UrlGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationResponse {
    private UUID id;
    private Double latitude;
    private Double longitude;
    private boolean checked;
    private boolean revisionRequested;
    private String speciesCommonName;
    private Double certainty;
    private ObservationType observationType;
    private String imageUrl;
    private Instant createdAt;

    public static ObservationResponse from(Observation observation, S3UrlGenerator urlGenerator) {
        return ObservationResponse.builder()
            .id(observation.getId())
            .latitude(observation.getLatitude())
            .longitude(observation.getLongitude())
            .checked(observation.isChecked())
            .revisionRequested(observation.isRevisionRequested())
            .speciesCommonName(observation.getSpeciesCommonName())
            .certainty(observation.getCertainty())
            .observationType(observation.getObservationType())
            .imageUrl(urlGenerator.generateUrl(observation.getReferencePictureKey()))
            .createdAt(observation.getCreatedAt())
            .build();
    }
}

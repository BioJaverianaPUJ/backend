package com.biojaveriana.api.observation.dto;

import java.util.UUID;

import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationType;

public record ObservationResponse(
    UUID id,
    String referencePictureUrl,
    double latitude,
    double longitude,
    ObservationType observationType,
    String speciesCommonName,
    double certainty,
    UUID specimenId,
    boolean checked,
    boolean revisionRequested
) {
    public static ObservationResponse from(Observation observation, S3UrlGenerator generator) {
        String referencePictureUrl = null;
        if (generator != null && observation.getReferencePictureKey() != null) {
            referencePictureUrl = generator.generateUrl(observation.getReferencePictureKey());
        }
        
        return new ObservationResponse(
            observation.getId(),
            referencePictureUrl,
            observation.getLatitude(),
            observation.getLongitude(),
            observation.getObservationType(),
            observation.getSpeciesCommonName(),
            observation.getCertainty(),
            observation.getSpecimen() != null ? observation.getSpecimen().getId() : null,
            observation.isChecked(),
            observation.isRevisionRequested()
        );
    }
}

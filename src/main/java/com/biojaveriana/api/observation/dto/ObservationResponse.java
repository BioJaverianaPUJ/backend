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
    boolean validationRequested
) {
    public static ObservationResponse from(Observation observation, S3UrlGenerator generator) {
        return new ObservationResponse(
            observation.getId(),
            generator.generateUrl(observation.getReferencePictureKey()),
            observation.getLatitude(),
            observation.getLongitude(),
            observation.getObservationType(),
            observation.getSpeciesCommonName(),
            observation.getCertainty(),
            observation.getSpecimen() != null ? observation.getSpecimen().getId() : null,
            observation.isChecked(),
            observation.isValidationRequested()
        );
    }
}

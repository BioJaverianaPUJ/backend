package com.biojaveriana.api.specimen.dto;

import com.biojaveriana.api.specimen.HealthStatus;
import com.biojaveriana.api.specimen.Specimen;
import java.time.Instant;
import java.util.UUID;

public record SpecimenResponse(
    UUID id,
    UUID speciesId,
    Double latitude,
    Double longitude,
    Double circumference,
    Double height,
    HealthStatus healthStatus,
    String notes,
    Instant observationDate,
    Instant createdAt
) {
    public static SpecimenResponse from(Specimen specimen) {
        return new SpecimenResponse(
            specimen.getId(),
            specimen.getSpecies().getId(),
            specimen.getLatitude(),
            specimen.getLongitude(),
            specimen.getCircumference(),
            specimen.getHeight(),
            specimen.getHealthStatus(),
            specimen.getNotes(),
            specimen.getObservationDate(),
            specimen.getCreatedAt()
        );
    }
}

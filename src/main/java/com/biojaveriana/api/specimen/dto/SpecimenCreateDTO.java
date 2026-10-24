package com.biojaveriana.api.specimen.dto;

import com.biojaveriana.api.specimen.HealthStatus;
import com.biojaveriana.api.specimen.Specimen;
import com.biojaveriana.api.specimen.Species;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record SpecimenCreateDTO(
        UUID speciesId,
        @NotNull Double latitude,
        @NotNull Double longitude,
        Double circumference,
        Double height,
        HealthStatus healthStatus,
        @NotBlank String notes,
        Instant observationDate
) {
    public Specimen toEntity(Species species) {
        return Specimen.builder()
            .species(species)
            .latitude(latitude)
            .longitude(longitude)
            .circumference(circumference)
            .height(height)
            .healthStatus(healthStatus)
            .notes(notes)
            .observationDate(observationDate)
            .build();
    }
}

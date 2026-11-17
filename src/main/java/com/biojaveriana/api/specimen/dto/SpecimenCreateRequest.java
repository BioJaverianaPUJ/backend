package com.biojaveriana.api.specimen.dto;

import com.biojaveriana.api.specimen.HealthStatus;
import com.biojaveriana.api.specimen.Species;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request body for creating a new specimen", example = "{\"speciesId\": \"550e8400-e29b-41d4-a716-446655440000\", \"latitude\": 4.5708, \"longitude\": -75.7521, \"circumference\": 150.5, \"height\": 25.0, \"healthStatus\": \"WELL\", \"notes\": \"Healthy specimen\", \"observationDate\": \"2026-06-15T10:00:00Z\"}")
public record SpecimenCreateRequest(
        @Schema(description = "ID of the associated species", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID speciesId,

        @Schema(description = "Latitude of the specimen location", example = "4.5708")
        @NotNull Double latitude,

        @Schema(description = "Longitude of the specimen location", example = "-75.7521")
        @NotNull Double longitude,

        @Schema(description = "Circumference at breast height (cm)", example = "150.5")
        Double circumference,

        @Schema(description = "Height of the specimen (m)", example = "25.0")
        Double height,

        @Schema(description = "Current health status", example = "WELL", allowableValues = {"WELL", "FAIR", "POOR", "DEAD"})
        HealthStatus healthStatus,

        @Schema(description = "Notes about the specimen", example = "Healthy specimen")
        @NotBlank String notes,

        @Schema(description = "Date the specimen was observed", example = "2026-06-15T10:00:00Z")
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

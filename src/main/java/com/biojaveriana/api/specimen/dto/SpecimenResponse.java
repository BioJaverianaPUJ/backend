package com.biojaveriana.api.specimen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.specimen.HealthStatus;
import com.biojaveriana.api.specimen.Specimen;

@Schema(description = "Response body containing specimen information", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"speciesId\": \"660e8400-e29b-41d4-a716-446655440001\", \"latitude\": 4.5708, \"longitude\": -75.7521, \"circumference\": 150.5, \"height\": 25.0, \"healthStatus\": \"WELL\", \"notes\": \"Healthy specimen\", \"observationDate\": \"2026-06-15T10:00:00Z\", \"createdAt\": \"2026-06-15T10:00:00Z\"}")
public record SpecimenResponse(
    @Schema(description = "Unique specimen identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "ID of the associated species", example = "660e8400-e29b-41d4-a716-446655440001")
    UUID speciesId,

    @Schema(description = "Latitude of the specimen location", example = "4.5708")
    Double latitude,

    @Schema(description = "Longitude of the specimen location", example = "-75.7521")
    Double longitude,

    @Schema(description = "Circumference at breast height (cm)", example = "150.5")
    Double circumference,

    @Schema(description = "Height of the specimen (m)", example = "25.0")
    Double height,

    @Schema(description = "Current health status", example = "WELL", allowableValues = {"WELL", "FAIR", "POOR", "DEAD"})
    HealthStatus healthStatus,

    @Schema(description = "Notes about the specimen", example = "Healthy specimen")
    String notes,

    @Schema(description = "Date the specimen was observed", example = "2026-06-15T10:00:00Z")
    Instant observationDate,

    @Schema(description = "Timestamp when the specimen was created", example = "2026-06-15T10:00:00Z")
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

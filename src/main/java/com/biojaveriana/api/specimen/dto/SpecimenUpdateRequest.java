package com.biojaveriana.api.specimen.dto;

import com.biojaveriana.api.specimen.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Request body for updating an existing specimen", example = "{\"latitude\": 4.5708, \"longitude\": -75.7521, \"circumference\": 151.0, \"height\": 25.5, \"healthStatus\": \"WELL\", \"notes\": \"Updated notes\", \"observationDate\": \"2026-06-16T10:00:00Z\"}")
public record SpecimenUpdateRequest(
    @Schema(description = "Latitude of the specimen location", example = "4.5708")
    Double latitude,

    @Schema(description = "Longitude of the specimen location", example = "-75.7521")
    Double longitude,

    @Schema(description = "Circumference at breast height (cm)", example = "151.0")
    Double circumference,

    @Schema(description = "Height of the specimen (m)", example = "25.5")
    Double height,

    @Schema(description = "Current health status", example = "WELL", allowableValues = {"WELL", "FAIR", "POOR", "DEAD"})
    HealthStatus healthStatus,

    @Schema(description = "Notes about the specimen", example = "Updated notes")
    String notes,

    @Schema(description = "Date the specimen was observed", example = "2026-06-16T10:00:00Z")
    Instant observationDate
) {}

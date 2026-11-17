package com.biojaveriana.api.specimen;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The health status of a specimen", example = "WELL")
public enum HealthStatus {
    @Schema(description = "Specimen is in good health")
    WELL,
    @Schema(description = "Specimen is in fair health")
    FAIR,
    @Schema(description = "Specimen is in poor health")
    POOR,
    @Schema(description = "Specimen is dead")
    DEAD
}

package com.biojaveriana.api.observation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The type of biodiversity observation", example = "PLANT")
public enum ObservationType {
    @Schema(description = "Plant observation")
    PLANT,
    @Schema(description = "Animal observation")
    ANIMAL,
    @Schema(description = "Fungi observation")
    FUNGI,
    @Schema(description = "Unknown observation type")
    UNKNOWN
}

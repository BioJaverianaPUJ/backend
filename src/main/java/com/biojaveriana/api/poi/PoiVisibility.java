package com.biojaveriana.api.poi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The visibility setting for a point of interest", example = "PUBLIC")
public enum PoiVisibility {
    @Schema(description = "Visible to all users")
    PUBLIC,
    @Schema(description = "Visible only to the creator")
    PRIVATE
}

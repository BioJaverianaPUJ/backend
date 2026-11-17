package com.biojaveriana.api.poi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The geometry type of a point of interest", example = "POLYGON")
public enum PoiType {
    @Schema(description = "Polygon-shaped area")
    POLYGON,
    @Schema(description = "Single point location")
    POINT
}

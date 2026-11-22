package com.biojaveriana.api.poi.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.biojaveriana.api.poi.PoiType;
import com.biojaveriana.api.poi.PoiVisibility;
import com.biojaveriana.api.poi.PoiZoneType;

public record PoiCreateRequest(
    @NotNull
    @NotBlank
    String name,

    String description,

    @NotNull
    PoiType geometryType,

    @NotNull
    PoiZoneType type,

    List<PolygonCoordinate> polygonCoordinates,

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double centerLatitude,

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double centerLongitude,

    @Positive
    Double radiusMeters,

    @NotNull
    PoiVisibility visibility
) {
    public record PolygonCoordinate(
        @NotNull
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        Double longitude
    ) {}
}

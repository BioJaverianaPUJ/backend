package com.biojaveriana.api.poi.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import com.biojaveriana.api.poi.PoiVisibility;
import com.biojaveriana.api.poi.PoiZoneType;

public record PoiUpdateRequest(
    String name,

    String description,

    List<PoiCreateRequest.PolygonCoordinate> polygonCoordinates,

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double centerLatitude,

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double centerLongitude,

    @Positive
    Double radiusMeters,

    PoiVisibility visibility,

    PoiZoneType type,

    Boolean active
) {}

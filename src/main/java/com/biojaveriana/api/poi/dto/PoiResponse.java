package com.biojaveriana.api.poi.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.poi.Poi;
import com.biojaveriana.api.poi.PoiType;
import com.biojaveriana.api.poi.PoiZoneType;

public record PoiResponse(
    UUID id,
    UUID creatorId,
    String name,
    String description,
    PoiType geometryType,
    PoiZoneType type,
    String polygonCoordinatesWkt,
    Double centerLatitude,
    Double centerLongitude,
    Double radiusMeters,
    String visibility,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static PoiResponse from(Poi poi) {
        return new PoiResponse(
            poi.getId(),
            poi.getCreator().getId(),
            poi.getName(),
            poi.getDescription(),
            poi.getGeometryType(),
            poi.getType(),
            poi.getPolygonCoordinatesWkt(),
            poi.getCenterLatitude(),
            poi.getCenterLongitude(),
            poi.getRadiusMeters(),
            poi.getVisibility() != null ? poi.getVisibility().name() : null,
            poi.getActive(),
            poi.getCreatedAt(),
            poi.getUpdatedAt()
        );
    }
}

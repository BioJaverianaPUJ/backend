package com.biojaveriana.api.motd.dto;

import java.util.UUID;

import com.biojaveriana.api.poi.Poi;
import com.biojaveriana.api.poi.PoiType;
import com.biojaveriana.api.poi.PoiZoneType;

public record PoiSummaryResponse(
    UUID id,
    String name,
    PoiType geometryType,
    PoiZoneType type,
    String polygonCoordinatesWkt,
    Double centerLatitude,
    Double centerLongitude,
    Double radiusMeters
) {
    public static PoiSummaryResponse from(Poi poi) {
        return new PoiSummaryResponse(
            poi.getId(),
            poi.getName(),
            poi.getGeometryType(),
            poi.getType(),
            poi.getPolygonCoordinatesWkt(),
            poi.getCenterLatitude(),
            poi.getCenterLongitude(),
            poi.getRadiusMeters()
        );
    }
}

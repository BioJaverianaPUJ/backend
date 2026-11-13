package com.biojaveriana.api.poi.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.poi.Poi;
import com.biojaveriana.api.poi.PoiType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiResponse {
    private UUID id;
    private UUID creatorId;
    private String name;
    private String description;
    private PoiType geometryType;
    private String polygonCoordinatesWkt;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radiusMeters;
    private String visibility;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static PoiResponse from(Poi poi) {
        return PoiResponse.builder()
            .id(poi.getId())
            .creatorId(poi.getCreator().getId())
            .name(poi.getName())
            .description(poi.getDescription())
            .geometryType(poi.getGeometryType())
            .polygonCoordinatesWkt(poi.getPolygonCoordinatesWkt())
            .centerLatitude(poi.getCenterLatitude())
            .centerLongitude(poi.getCenterLongitude())
            .radiusMeters(poi.getRadiusMeters())
            .visibility(poi.getVisibility() != null ? poi.getVisibility().name() : null)
            .active(poi.getActive())
            .createdAt(poi.getCreatedAt())
            .updatedAt(poi.getUpdatedAt())
            .build();
    }
}

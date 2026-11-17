package com.biojaveriana.api.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.poi.Poi;
import com.biojaveriana.api.poi.PoiType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Response body containing POI details", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"creatorId\": \"660e8400-e29b-41d4-a716-446655440001\", \"name\": \"Campus Main Gate\", \"description\": \"Main entrance to the campus\", \"geometryType\": \"POINT\", \"polygonCoordinatesWkt\": null, \"centerLatitude\": 4.5708, \"centerLongitude\": -75.7521, \"radiusMeters\": 50.0, \"visibility\": \"PUBLIC\", \"active\": true, \"createdAt\": \"2026-06-15T10:00:00Z\", \"updatedAt\": \"2026-06-15T10:00:00Z\"}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiResponse {
    @Schema(description = "Unique POI identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "ID of the user who created this POI", example = "660e8400-e29b-41d4-a716-446655440001")
    private UUID creatorId;

    @Schema(description = "Name of the POI", example = "Campus Main Gate")
    private String name;

    @Schema(description = "Description of the POI", example = "Main entrance to the campus")
    private String description;

    @Schema(description = "Geometry type of the POI", example = "POINT", allowableValues = {"POLYGON", "POINT"})
    private PoiType geometryType;

    @Schema(description = "WKT polygon coordinates (for POLYGON type)", example = null)
    private String polygonCoordinatesWkt;

    @Schema(description = "Latitude of the center point", example = "4.5708")
    private Double centerLatitude;

    @Schema(description = "Longitude of the center point", example = "-75.7521")
    private Double centerLongitude;

    @Schema(description = "Radius in meters (for POINT type)", example = "50.0")
    private Double radiusMeters;

    @Schema(description = "Visibility setting", example = "PUBLIC")
    private String visibility;

    @Schema(description = "Whether the POI is currently active", example = "true")
    private Boolean active;

    @Schema(description = "Timestamp when the POI was created", example = "2026-06-15T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when the POI was last updated", example = "2026-06-15T10:00:00Z")
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

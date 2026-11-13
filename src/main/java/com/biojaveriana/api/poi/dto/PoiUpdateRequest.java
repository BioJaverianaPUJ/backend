package com.biojaveriana.api.poi.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import com.biojaveriana.api.poi.PoiVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiUpdateRequest {
    private String name;

    private String description;

    private List<PoiCreateRequest.PolygonCoordinate> polygonCoordinates;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double centerLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double centerLongitude;

    @Positive
    private Double radiusMeters;

    private PoiVisibility visibility;

    private Boolean active;
}
